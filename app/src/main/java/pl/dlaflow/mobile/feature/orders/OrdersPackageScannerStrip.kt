package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.dlaflow.mobile.MobilePackageScanUiState
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.packageScannerResolvedCopy
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton

private data class ScannerStripPresentation(
    val title: String,
    val supportingText: String,
    val orderStatus: String? = null,
    val orderNumber: String? = null,
    val loading: Boolean = false,
    val retryable: Boolean = false,
)

@Composable
internal fun OrdersPackageScannerStrip(
    colors: DlaFlowComposeColors,
    scanState: MobilePackageScanUiState,
    onOpenOrder: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    if (scanState is MobilePackageScanUiState.Empty) return

    val presentation = scannerStripPresentation(scanState)
    DlaFlowCard(colors, accent = !presentation.loading) {
        BoxWithConstraints(Modifier.testTag("orders_package_scanner_strip")) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.2f
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScannerStripCopy(colors, presentation, Modifier.fillMaxWidth())
                    ScannerStripAction(colors, presentation, Modifier.fillMaxWidth(), onOpenOrder, onScanAgain)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScannerStripCopy(colors, presentation, Modifier.weight(1f))
                    ScannerStripAction(colors, presentation, Modifier, onOpenOrder, onScanAgain)
                }
            }
        }
    }
}

@Composable
private fun scannerStripPresentation(scanState: MobilePackageScanUiState): ScannerStripPresentation = when (scanState) {
    MobilePackageScanUiState.Empty -> error("Empty is handled before presentation")
    is MobilePackageScanUiState.Loading -> ScannerStripPresentation(
        title = stringResource(R.string.orders_scanner_checking),
        supportingText = stringResource(R.string.orders_scanner_checking_description),
        loading = true,
    )
    is MobilePackageScanUiState.Failed -> ScannerStripPresentation(
            title = stringResource(R.string.orders_scanner_failed),
        supportingText = scanState.message,
        retryable = true,
    )
    is MobilePackageScanUiState.Resolved -> {
        val copy = packageScannerResolvedCopy(scanState.result)
        val order = scanState.result.order.takeIf { scanState.result.matched }
        ScannerStripPresentation(
            title = copy.title,
            supportingText = copy.supportingText,
            orderStatus = order?.let { stringResource(R.string.orders_scanner_order_status, it.orderNumber, it.status) },
            orderNumber = order?.orderNumber,
            retryable = order == null,
        )
    }
}

@Composable
private fun ScannerStripCopy(
    colors: DlaFlowComposeColors,
    presentation: ScannerStripPresentation,
    modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (presentation.loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = colors.primary, strokeWidth = 2.dp)
        } else {
            DlaFlowIcon(Icons.Rounded.QrCodeScanner, colors.primary, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(text = presentation.title, color = colors.textStrong, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text(text = presentation.supportingText, color = colors.textMuted, fontSize = 12.sp, letterSpacing = 0.sp)
            presentation.orderStatus?.let { Text(text = it, color = colors.textMuted, fontSize = 12.sp, letterSpacing = 0.sp) }
        }
    }
}

@Composable
private fun ScannerStripAction(
    colors: DlaFlowComposeColors,
    presentation: ScannerStripPresentation,
    modifier: Modifier,
    onOpenOrder: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    when {
        presentation.orderNumber != null -> DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            text = stringResource(R.string.orders_scanner_open_order),
            modifier = modifier,
            onClick = { onOpenOrder(presentation.orderNumber) },
        )
        presentation.retryable -> DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.QrCodeScanner,
            text = stringResource(R.string.orders_scanner_retry),
            modifier = modifier,
            onClick = onScanAgain,
        )
    }
}
