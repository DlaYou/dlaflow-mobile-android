package pl.dlaflow.mobile.core.state

internal data class DlaFlowUiMessage(
    val titleRes: Int,
    val descriptionRes: Int,
    val retryable: Boolean,
)

internal sealed interface DlaFlowUiState<out T> {
    data object Loading : DlaFlowUiState<Nothing>
    data class Content<T>(val data: T) : DlaFlowUiState<T>
    data object Empty : DlaFlowUiState<Nothing>
    data class Error(val message: DlaFlowUiMessage) : DlaFlowUiState<Nothing>
    data class Offline<T>(val lastContent: T? = null) : DlaFlowUiState<T>
    data object NoAccess : DlaFlowUiState<Nothing>
}
