package pl.dlaflow.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowPrimaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusBadge
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusStrip

@Composable
internal fun SessionRecoveryFeatureScreen(
    colors: DlaFlowComposeColors,
    state: MobileSessionUiState,
    session: MobileSession?,
    onRetry: () -> Unit,
) {
    val checking = state == MobileSessionUiState.CHECKING
    val supportingText = stringResource(
        if (checking) R.string.mobile_session_recovery_checking
        else R.string.mobile_session_recovery_offline_description,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        DlaFlowIcon(
            icon = Icons.Rounded.PhoneAndroid,
            color = colors.primary,
            modifier = Modifier.size(68.dp),
            contentDescription = stringResource(R.string.mobile_session_recovery_device),
        )
        Text(
            text = stringResource(R.string.mobile_session_recovery_title),
            color = colors.textStrong,
            fontSize = 23.sp,
            lineHeight = 29.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = supportingText,
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        DlaFlowCard(colors, accent = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session?.deviceName?.ifBlank { "Telefon" } ?: "Telefon",
                        color = colors.textStrong,
                        fontSize = 16.sp,
                        fontFamily = DlaFlowInter,
                        fontWeight = FontWeight.Bold,
                    )
                    val tenant = session?.tenantName.orEmpty()
                    if (tenant.isNotBlank()) {
                        Text(
                            text = tenant,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = DlaFlowInter,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                if (checking) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    DlaFlowStatusBadge(colors, stringResource(R.string.mobile_error_offline_title))
                }
            }
        }
        DlaFlowPrimaryButton(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            text = stringResource(R.string.mobile_session_recovery_retry),
            modifier = Modifier.fillMaxWidth(),
            enabled = !checking,
            onClick = onRetry,
        )
        if (!checking) {
            DlaFlowStatusStrip(colors, supportingText)
        }
    }
}
