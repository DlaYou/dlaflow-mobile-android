package pl.dlaflow.mobile.feature.dashboard

import java.util.concurrent.Executor

internal class DashboardCoordinator(
    private val stateHolder: DashboardStateHolder,
    private val gateway: DashboardGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onFeedback: (DashboardFeedback) -> Unit,
    private val onSessionRevoked: () -> Unit,
) {
    fun refresh(token: String, showFeedback: Boolean) {
        val request = stateHolder.beginLoad(token)
        if (showFeedback) onFeedback(DashboardFeedback.REFRESHING)

        executor.execute {
            runCatching {
                gateway.load(token).toDashboardContent()
            }.onSuccess { content ->
                postToMain {
                    if (stateHolder.acceptSuccess(request, content) && showFeedback) {
                        onFeedback(DashboardFeedback.REFRESHED)
                    }
                }
            }.onFailure { error ->
                val failure = mapDashboardFailure(error)
                postToMain {
                    when (failure) {
                        DashboardFailure.Unauthorized -> {
                            if (stateHolder.acceptUnauthorized(request)) onSessionRevoked()
                        }

                        DashboardFailure.NoAccess -> {
                            emitFailureFeedbackIfAccepted(
                                accepted = stateHolder.acceptNoAccess(request),
                                showFeedback = showFeedback,
                            )
                        }

                        is DashboardFailure.Offline -> {
                            emitFailureFeedbackIfAccepted(
                                accepted = stateHolder.acceptOffline(request, failure.message),
                                showFeedback = showFeedback,
                            )
                        }

                        is DashboardFailure.Retryable -> {
                            emitFailureFeedbackIfAccepted(
                                accepted = stateHolder.acceptFailure(request, failure.message),
                                showFeedback = showFeedback,
                            )
                        }
                    }
                }
            }
        }
    }

    fun reset() {
        stateHolder.reset()
    }

    private fun emitFailureFeedbackIfAccepted(accepted: Boolean, showFeedback: Boolean) {
        if (accepted && showFeedback) onFeedback(DashboardFeedback.LOAD_FAILED)
    }
}
