package pl.dlaflow.mobile.feature.notifications

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.MobileAssistantNotification
import pl.dlaflow.mobile.MobileNotificationAction
import pl.dlaflow.mobile.MobilePhotoTask
import pl.dlaflow.mobile.core.session.NotificationSessionKey
import pl.dlaflow.mobile.core.session.NotificationSessionSynchronization

class NotificationsBackgroundCoordinatorTest {
    @Test
    fun `stale session after fetch shows and persists nothing for photo and panel alerts`() {
        val coordinator = coordinator()
        val sessionA = key("device-a", "token-a")
        val sessionB = key("device-b", "token-b")
        val currentSession = AtomicReference(sessionA)
        val memory = FakeMemory()
        val shownPhotoTasks = mutableListOf<String>()
        val shownPanelAlerts = mutableListOf<String>()

        val outcome = coordinator.poll(
            capturedSessionKey = sessionA,
            currentSessionKey = { currentSession.get() },
            memory = memory,
            loadPhotoTask = {
                currentSession.set(sessionB)
                photoTask("photo-a")
            },
            loadPanelNotifications = { listOf(panelAlert("panel-a")) },
            showPhotoTask = { shownPhotoTasks += it.id; true },
            showPanelAlert = { shownPanelAlerts += it.id; true },
        )

        assertEquals(NotificationsBackgroundPollOutcome.SKIPPED_STALE_SESSION, outcome)
        assertTrue(shownPhotoTasks.isEmpty())
        assertTrue(shownPanelAlerts.isEmpty())
        assertEquals("", memory.photoTaskId)
        assertEquals("", memory.panelAlertIds)
    }

    @Test
    fun `session changed while loading panel alerts rejects every fetched alert`() {
        val coordinator = coordinator()
        val sessionA = key("device-a", "token-a")
        val sessionB = key("device-b", "token-b")
        val currentSession = AtomicReference(sessionA)
        val memory = FakeMemory()
        val attempts = AtomicInteger()

        val outcome = coordinator.poll(
            capturedSessionKey = sessionA,
            currentSessionKey = { currentSession.get() },
            memory = memory,
            loadPhotoTask = { null },
            loadPanelNotifications = {
                currentSession.set(sessionB)
                listOf(panelAlert("panel-a"), panelAlert("panel-b"))
            },
            showPhotoTask = { true },
            showPanelAlert = { attempts.incrementAndGet(); true },
        )

        assertEquals(NotificationsBackgroundPollOutcome.SKIPPED_STALE_SESSION, outcome)
        assertEquals(0, attempts.get())
        assertEquals("", memory.panelAlertIds)
    }

    @Test
    fun `foreground and job overlap share one in flight poll`() {
        val coordinator = coordinator()
        val session = key("device-a", "token-a")
        val memory = FakeMemory()
        val fetchStarted = CountDownLatch(1)
        val releaseFetch = CountDownLatch(1)
        val fetchCount = AtomicInteger()
        val firstOutcome = AtomicReference<NotificationsBackgroundPollOutcome>()
        val executor = Executors.newSingleThreadExecutor()

        try {
            executor.submit {
                firstOutcome.set(
                    coordinator.poll(
                        capturedSessionKey = session,
                        currentSessionKey = { session },
                        memory = memory,
                        loadPhotoTask = {
                            fetchCount.incrementAndGet()
                            fetchStarted.countDown()
                            releaseFetch.await(2, TimeUnit.SECONDS)
                            null
                        },
                        loadPanelNotifications = { emptyList() },
                        showPhotoTask = { true },
                        showPanelAlert = { true },
                    ),
                )
            }
            assertTrue(fetchStarted.await(2, TimeUnit.SECONDS))

            val overlapping = coordinator.poll(
                capturedSessionKey = session,
                currentSessionKey = { session },
                memory = memory,
                loadPhotoTask = { fetchCount.incrementAndGet(); null },
                loadPanelNotifications = { emptyList() },
                showPhotoTask = { true },
                showPanelAlert = { true },
            )

            assertEquals(NotificationsBackgroundPollOutcome.SKIPPED_IN_FLIGHT, overlapping)
            assertEquals(1, fetchCount.get())
            releaseFetch.countDown()
            executor.shutdown()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
            assertEquals(NotificationsBackgroundPollOutcome.COMPLETED, firstOutcome.get())
        } finally {
            releaseFetch.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `failed native effects release claims and can be retried`() {
        val coordinator = coordinator()
        val session = key("device-a", "token-a")
        val memory = FakeMemory()
        val photoAttempts = AtomicInteger()
        val panelAttempts = AtomicInteger()

        repeat(2) {
            coordinator.poll(
                capturedSessionKey = session,
                currentSessionKey = { session },
                memory = memory,
                loadPhotoTask = { photoTask("photo-a") },
                loadPanelNotifications = { listOf(panelAlert("panel-a")) },
                showPhotoTask = { photoAttempts.incrementAndGet(); false },
                showPanelAlert = { panelAttempts.incrementAndGet(); false },
            )
        }

        assertEquals(2, photoAttempts.get())
        assertEquals(2, panelAttempts.get())
        assertEquals("", memory.photoTaskId)
        assertEquals("", memory.panelAlertIds)
    }

    @Test
    fun `throwing native effects also release claims`() {
        val coordinator = coordinator()
        val session = key("device-a", "token-a")
        val memory = FakeMemory()
        val photoAttempts = AtomicInteger()
        val panelAttempts = AtomicInteger()

        repeat(2) {
            coordinator.poll(
                capturedSessionKey = session,
                currentSessionKey = { session },
                memory = memory,
                loadPhotoTask = { photoTask("photo-a") },
                loadPanelNotifications = { listOf(panelAlert("panel-a")) },
                showPhotoTask = {
                    photoAttempts.incrementAndGet()
                    error("platform photo failure")
                },
                showPanelAlert = {
                    panelAttempts.incrementAndGet()
                    error("platform panel failure")
                },
            )
        }

        assertEquals(2, photoAttempts.get())
        assertEquals(2, panelAttempts.get())
        assertEquals("", memory.photoTaskId)
        assertEquals("", memory.panelAlertIds)
    }

    @Test
    fun `successful effects are claimed once and every unread panel alert is delivered`() {
        val coordinator = coordinator()
        val session = key("device-a", "token-a")
        val memory = FakeMemory()
        val photoAttempts = AtomicInteger()
        val panelAttempts = AtomicInteger()

        repeat(2) {
            coordinator.poll(
                capturedSessionKey = session,
                currentSessionKey = { session },
                memory = memory,
                loadPhotoTask = { photoTask("photo-a") },
                loadPanelNotifications = {
                    listOf(
                        panelAlert("panel-a"),
                        panelAlert("read-panel", readAt = "2026-07-18T10:00:00Z"),
                        panelAlert("quiet-panel", tone = "success", actionType = "OPEN_LOGS_SUMMARY"),
                    )
                },
                showPhotoTask = { photoAttempts.incrementAndGet(); true },
                showPanelAlert = { panelAttempts.incrementAndGet(); true },
            )
        }

        assertEquals(1, photoAttempts.get())
        assertEquals(2, panelAttempts.get())
        assertEquals("photo-a", memory.photoTaskId)
        assertTrue(memory.panelAlertIds.contains("panel-a"))
        assertFalse(memory.panelAlertIds.contains("read-panel"))
        assertTrue(memory.panelAlertIds.contains("quiet-panel"))
    }

    @Test
    fun `session transition cannot interleave between validation claim and native effect`() {
        val synchronization = NotificationSessionSynchronization()
        val coordinator = NotificationsBackgroundCoordinator(synchronization)
        val sessionA = key("device-a", "token-a")
        val sessionB = key("device-b", "token-b")
        val currentSession = AtomicReference(sessionA)
        val memory = FakeMemory()
        val effectStarted = CountDownLatch(1)
        val releaseEffect = CountDownLatch(1)
        val transitionFinished = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            executor.submit {
                coordinator.poll(
                    capturedSessionKey = sessionA,
                    currentSessionKey = { currentSession.get() },
                    memory = memory,
                    loadPhotoTask = { photoTask("photo-a") },
                    loadPanelNotifications = { emptyList() },
                    showPhotoTask = {
                        effectStarted.countDown()
                        releaseEffect.await(2, TimeUnit.SECONDS)
                        true
                    },
                    showPanelAlert = { true },
                )
            }
            assertTrue(effectStarted.await(2, TimeUnit.SECONDS))
            executor.submit {
                synchronization.withLock {
                    currentSession.set(sessionB)
                }
                transitionFinished.countDown()
            }

            assertFalse(transitionFinished.await(150, TimeUnit.MILLISECONDS))
            releaseEffect.countDown()
            assertTrue(transitionFinished.await(2, TimeUnit.SECONDS))
            assertEquals(sessionB, currentSession.get())
            assertEquals("photo-a", memory.photoTaskId)
        } finally {
            releaseEffect.countDown()
            executor.shutdownNow()
        }
    }

    private fun coordinator() = NotificationsBackgroundCoordinator(NotificationSessionSynchronization())

    private fun key(deviceId: String, token: String) = requireNotNull(
        NotificationSessionKey.create("https://panel.example.test", deviceId, token),
    )

    private fun photoTask(id: String) = MobilePhotoTask(
        id = id,
        productName = "Produkt",
        productSku = "SKU-1",
        status = "PENDING",
        mediaCount = 0,
        maxPhotos = 3,
        expiresAt = "2026-07-19T10:00:00Z",
    )

    private fun panelAlert(
        id: String,
        readAt: String? = null,
        tone: String = "warning",
        actionType: String = "OPEN_ORDERS",
    ) = MobileAssistantNotification(
        id = id,
        title = "Wymaga uwagi",
        description = "Sprawdź sprawę w panelu.",
        tone = tone,
        source = "DlaFlow",
        account = "Panel",
        occurredAt = "2026-07-18T10:00:00Z",
        readAt = readAt,
        mobileAction = MobileNotificationAction(actionType, "Otwórz"),
    )

    private class FakeMemory : NotificationsBackgroundDeliveryMemory {
        var photoTaskId = ""
        var panelAlertIds = ""

        override fun readLastPhotoTaskId(): String = photoTaskId

        override fun saveLastPhotoTaskId(taskId: String) {
            photoTaskId = taskId
        }

        override fun readShownPanelAlertIds(): String = panelAlertIds

        override fun saveShownPanelAlertIds(ids: String) {
            panelAlertIds = ids
        }
    }
}
