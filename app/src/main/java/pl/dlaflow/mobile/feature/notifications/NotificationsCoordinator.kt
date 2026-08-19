package pl.dlaflow.mobile.feature.notifications

import java.util.concurrent.Executor

internal class NotificationsCoordinator(
    private val stateHolder: NotificationsStateHolder,
    private val gateway: NotificationsGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onEffect: (NotificationsEffect) -> Unit,
    private val onReadStateChanged: () -> Unit,
    private val onUnauthorized: (Throwable, Boolean, () -> Unit, () -> Unit) -> Unit,
) {
    fun open(
        token: String,
        dashboardPreview: List<NotificationItem>,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        stateHolder.setDashboardPreview(dashboardPreview)
        return refresh(token, allowUnauthorizedRetry)
    }

    fun refresh(
        token: String,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        val request = stateHolder.beginLoad(token) ?: return false
        executeLoad(token, request, allowUnauthorizedRetry)
        return true
    }

    fun selectFilter(filter: NotificationFilter) {
        stateHolder.selectFilter(filter)
    }

    fun markVisibleRead(
        token: String,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        val request = stateHolder.beginMarkVisibleRead(token) ?: return false
        executeMutation(token, request, allowUnauthorizedRetry)
        return true
    }

    fun openItem(notificationId: String): Boolean {
        val effect = stateHolder.effectFor(notificationId) ?: return false
        onEffect(effect)
        return true
    }

    fun reset() {
        stateHolder.reset()
    }

    private fun executeLoad(
        token: String,
        request: NotificationsLoadRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching { gateway.load(token) }
                .onSuccess { content ->
                    postToMain { stateHolder.acceptLoadSuccess(request, content) }
                }
                .onFailure { error ->
                    postToMain { handleLoadFailure(token, request, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleLoadFailure(
        token: String,
        request: NotificationsLoadRequest,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapNotificationsFailure(error)) {
            is NotificationsFailure.Unauthorized -> {
                val terminalMessage = failure.message.takeIf { !allowUnauthorizedRetry }
                if (stateHolder.acceptLoadUnauthorized(request, terminalMessage)) {
                    onUnauthorized(
                        error,
                        allowUnauthorizedRetry,
                        {
                            stateHolder.beginLoadUnauthorizedRetry(request)?.let { retry ->
                                executeLoad(token, retry, allowUnauthorizedRetry = false)
                            }
                        },
                        {
                            stateHolder.acceptLoadSessionUnconfirmed(
                                request,
                                notificationsSessionUnconfirmedMessage(),
                            )
                        },
                    )
                }
            }

            is NotificationsFailure.NoAccess -> stateHolder.acceptLoadNoAccess(request)
            is NotificationsFailure.Offline -> stateHolder.acceptLoadOffline(request, failure.message)
            is NotificationsFailure.Retryable -> stateHolder.acceptLoadFailure(request, failure.message)
        }
    }

    private fun executeMutation(
        token: String,
        request: NotificationsMutationRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching { gateway.markRead(token, request.notificationIds) }
                .onSuccess {
                    postToMain {
                        if (stateHolder.acceptMutationSuccess(request)) {
                            onReadStateChanged()
                            refresh(token)
                        }
                    }
                }
                .onFailure { error ->
                    postToMain { handleMutationFailure(token, request, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleMutationFailure(
        token: String,
        request: NotificationsMutationRequest,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapNotificationsFailure(error)) {
            is NotificationsFailure.Unauthorized -> {
                val terminalMessage = failure.message.takeIf { !allowUnauthorizedRetry }
                if (stateHolder.acceptMutationUnauthorized(request, terminalMessage)) {
                    onUnauthorized(
                        error,
                        allowUnauthorizedRetry,
                        {
                            stateHolder.beginMutationUnauthorizedRetry(request)?.let { retry ->
                                executeMutation(token, retry, allowUnauthorizedRetry = false)
                            }
                        },
                        {
                            stateHolder.acceptMutationSessionUnconfirmed(
                                request,
                                notificationsSessionUnconfirmedMessage(),
                            )
                        },
                    )
                }
            }

            is NotificationsFailure.NoAccess -> stateHolder.acceptMutationNoAccess(request)
            is NotificationsFailure.Offline -> stateHolder.acceptMutationOffline(request, failure.message)
            is NotificationsFailure.Retryable -> stateHolder.acceptMutationFailure(request, failure.message)
        }
    }
}
