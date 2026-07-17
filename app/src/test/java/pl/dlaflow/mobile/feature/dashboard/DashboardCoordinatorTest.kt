package pl.dlaflow.mobile.feature.dashboard

import java.net.ConnectException
import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

class DashboardCoordinatorTest {
    @Test
    fun `successful refresh maps response and emits refreshed feedback`() {
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { dashboardDto(todayRevenue = 512.0) },
        )

        harness.coordinator.refresh("session-a", showFeedback = true)

        assertEquals(DlaFlowUiState.Loading, harness.holder.state.contentState)
        assertEquals(listOf(DashboardFeedback.REFRESHING), harness.feedback)

        harness.executor.runNext()

        assertEquals(DlaFlowUiState.Loading, harness.holder.state.contentState)
        assertEquals(1, harness.mainQueue.size)
        assertEquals(listOf(DashboardFeedback.REFRESHING), harness.feedback)

        harness.mainQueue.runNext()

        assertEquals(512.0, harness.holder.state.contentOrNull()!!.todayRevenue, 0.0)
        assertEquals(
            listOf(DashboardFeedback.REFRESHING, DashboardFeedback.REFRESHED),
            harness.feedback,
        )
    }

    @Test
    fun `latest refresh wins when callbacks complete out of order`() {
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { session ->
                when (session) {
                    "session-old" -> dashboardDto(todayRevenue = 100.0)
                    else -> dashboardDto(todayRevenue = 300.0)
                }
            },
        )

        harness.coordinator.refresh("session-old", showFeedback = false)
        harness.coordinator.refresh("session-new", showFeedback = false)

        harness.executor.runLast()
        harness.executor.runNext()
        harness.mainQueue.runAll()

        assertEquals(300.0, harness.holder.state.contentOrNull()!!.todayRevenue, 0.0)
        assertTrue(harness.feedback.isEmpty())
        assertTrue(harness.unauthorizedEvents.isEmpty())
    }

    @Test
    fun `offline refresh retains last content`() {
        var loadCount = 0
        val harness = CoordinatorHarness(
            gateway = DashboardGateway {
                if (loadCount++ == 0) dashboardDto(todayRevenue = 350.0)
                else throw UnknownHostException("offline")
            },
        )
        harness.coordinator.refresh("session-a", showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val previousContent = harness.holder.state.contentOrNull()!!

        harness.coordinator.refresh("session-a", showFeedback = true)
        harness.executor.runNext()

        assertEquals(DlaFlowUiState.Content(previousContent), harness.holder.state.contentState)
        assertTrue(harness.holder.state.isRefreshing)

        harness.mainQueue.runNext()

        assertEquals(DlaFlowUiState.Offline(previousContent), harness.holder.state.contentState)
        assertEquals(
            mobileErrorToUiMessage(UnknownHostException("offline")),
            harness.holder.state.transientMessage,
        )
        assertEquals(
            listOf(DashboardFeedback.REFRESHING, DashboardFeedback.LOAD_FAILED),
            harness.feedback,
        )
    }

    @Test
    fun `forbidden response becomes no access`() {
        val harness = CoordinatorHarness(
            gateway = DashboardGateway {
                throw MobileApiException(403, "MOBILE_DEVICE_REQUIRED", "forbidden")
            },
        )

        harness.coordinator.refresh("session-a", showFeedback = true)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(DlaFlowUiState.NoAccess, harness.holder.state.contentState)
        assertEquals(
            listOf(DashboardFeedback.REFRESHING, DashboardFeedback.LOAD_FAILED),
            harness.feedback,
        )
        assertTrue(harness.unauthorizedEvents.isEmpty())
    }

    @Test
    fun `only accepted current unauthorized response clears feature and emits original error`() {
        val staleError = MobileApiException(401, "AUTH_REQUIRED", "stale")
        val currentError = MobileApiException(401, "AUTH_REQUIRED", "current")
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { session ->
                throw if (session == "session-old") staleError else currentError
            },
        )

        harness.coordinator.refresh("session-old", showFeedback = false)
        harness.coordinator.refresh("session-current", showFeedback = false)
        harness.executor.runNext()
        harness.executor.runNext()
        harness.mainQueue.runAll()

        assertEquals(DashboardUiState(), harness.holder.state)
        assertEquals(1, harness.unauthorizedEvents.size)
        assertSame(currentError, harness.unauthorizedEvents.single().error)
        assertTrue(harness.unauthorizedEvents.single().allowRetry)
        assertTrue(harness.feedback.isEmpty())
    }

    @Test
    fun `second unauthorized in the same retry chain carries exhausted retry budget`() {
        val firstError = MobileApiException(401, "AUTH_REQUIRED", "first")
        val retryError = MobileApiException(401, "AUTH_REQUIRED", "retry")
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { token ->
                throw if (token == "initial") firstError else retryError
            },
        )

        harness.coordinator.refresh("initial", showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.coordinator.refresh(
            token = "retry",
            showFeedback = false,
            allowUnauthorizedRetry = false,
        )
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(listOf(true, false), harness.unauthorizedEvents.map { it.allowRetry })
        assertSame(firstError, harness.unauthorizedEvents.first().error)
        assertSame(retryError, harness.unauthorizedEvents.last().error)
    }

    @Test
    fun `stale retry chain cannot consume retry budget of next independent request`() {
        val staleRetryError = MobileApiException(401, "AUTH_REQUIRED", "stale-retry")
        val independentError = MobileApiException(401, "AUTH_REQUIRED", "independent")
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { token ->
                throw if (token == "retry-chain") staleRetryError else independentError
            },
        )

        harness.coordinator.refresh(
            token = "retry-chain",
            showFeedback = false,
            allowUnauthorizedRetry = false,
        )
        harness.coordinator.refresh("independent", showFeedback = false)
        harness.executor.runNext()
        harness.executor.runNext()
        harness.mainQueue.runAll()

        assertEquals(1, harness.unauthorizedEvents.size)
        assertSame(independentError, harness.unauthorizedEvents.single().error)
        assertTrue(harness.unauthorizedEvents.single().allowRetry)
    }

    @Test
    fun `reset before callback prevents state and effect delivery`() {
        val harness = CoordinatorHarness(
            gateway = DashboardGateway { dashboardDto(todayRevenue = 900.0) },
        )

        harness.coordinator.refresh("session-a", showFeedback = true)
        harness.coordinator.reset()
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(DashboardUiState(), harness.holder.state)
        assertEquals(listOf(DashboardFeedback.REFRESHING), harness.feedback)
        assertTrue(harness.unauthorizedEvents.isEmpty())
    }

    @Test
    fun `failure classifier preserves typed authorization offline and retryable outcomes`() {
        val offline = UnknownHostException("offline")
        val disconnected = ConnectException("disconnected")
        val remaining = IllegalStateException("unexpected")

        assertEquals(
            DashboardFailure.Unauthorized,
            mapDashboardFailure(MobileApiException(401, "AUTH_REQUIRED", "expired")),
        )
        assertEquals(
            DashboardFailure.NoAccess,
            mapDashboardFailure(MobileApiException(403, "MOBILE_DEVICE_REQUIRED", "forbidden")),
        )
        assertEquals(
            DashboardFailure.Offline(mobileErrorToUiMessage(offline)),
            mapDashboardFailure(offline),
        )
        assertEquals(
            DashboardFailure.Offline(mobileErrorToUiMessage(disconnected)),
            mapDashboardFailure(disconnected),
        )
        assertEquals(
            DashboardFailure.Retryable(mobileErrorToUiMessage(remaining)),
            mapDashboardFailure(remaining),
        )
    }
}

private class CoordinatorHarness(gateway: DashboardGateway) {
    val holder = DashboardStateHolder()
    val executor = QueuedExecutor()
    val mainQueue = MainQueue()
    val feedback = mutableListOf<DashboardFeedback>()
    val unauthorizedEvents = mutableListOf<UnauthorizedEvent>()

    val coordinator = DashboardCoordinator(
        stateHolder = holder,
        gateway = gateway,
        executor = executor,
        postToMain = mainQueue::post,
        onFeedback = feedback::add,
        onUnauthorized = { error, allowRetry ->
            unauthorizedEvents += UnauthorizedEvent(error, allowRetry)
        },
    )
}

private data class UnauthorizedEvent(
    val error: Throwable,
    val allowRetry: Boolean,
)

private class QueuedExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()

    override fun execute(command: Runnable) {
        tasks.addLast(command)
    }

    fun runNext() {
        tasks.removeFirst().run()
    }

    fun runLast() {
        tasks.removeLast().run()
    }
}

private class MainQueue {
    private val tasks = ArrayDeque<() -> Unit>()

    val size: Int
        get() = tasks.size

    fun post(task: () -> Unit) {
        tasks.addLast(task)
    }

    fun runNext() {
        tasks.removeFirst().invoke()
    }

    fun runAll() {
        while (tasks.isNotEmpty()) runNext()
    }
}
