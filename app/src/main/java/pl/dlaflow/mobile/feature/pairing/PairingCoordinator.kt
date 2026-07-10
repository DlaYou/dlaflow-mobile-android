package pl.dlaflow.mobile.feature.pairing

import java.util.concurrent.Executor
import pl.dlaflow.mobile.MobileSession

internal class PairingCoordinator(
    private val stateHolder: PairingStateHolder,
    private val gateway: PairingGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onStarted: () -> Unit,
    private val onSuccess: (String, MobileSession) -> Unit,
    private val onFailure: () -> Unit,
) {
    fun submit(baseUrl: String) {
        val submission = stateHolder.beginSubmission() ?: return
        onStarted()
        executor.execute {
            runCatching {
                gateway.completePairing(baseUrl, submission)
            }.onSuccess { session ->
                postToMain {
                    if (stateHolder.acceptSuccess(submission.requestId)) {
                        onSuccess(baseUrl, session)
                    }
                }
            }.onFailure { error ->
                val failure = mapPairingFailure(error)
                postToMain {
                    val applied = when (failure) {
                        is PairingFailure.CodeRejected -> stateHolder.rejectCode(submission.requestId, failure.feedback)
                        is PairingFailure.Retryable -> stateHolder.failRetryable(submission.requestId, failure.message)
                    }
                    if (applied) onFailure()
                }
            }
        }
    }
}
