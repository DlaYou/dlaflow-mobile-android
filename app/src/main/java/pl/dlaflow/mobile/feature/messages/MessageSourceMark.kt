package pl.dlaflow.mobile.feature.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors

internal enum class MessageSourceAsset {
    ALLEGRO,
    GMAIL,
    WOOCOMMERCE,
}

internal data class MessageSourceMark(
    val label: String,
    val lightAsset: MessageSourceAsset?,
    val darkAsset: MessageSourceAsset? = lightAsset,
)

internal fun resolveMessageSourceMark(providerId: String, fallbackLabel: String = ""): MessageSourceMark {
    val normalizedProviderId = providerId.trim().take(64).lowercase(Locale.ROOT)
    return when (normalizedProviderId) {
        "allegro" -> MessageSourceMark("Allegro", MessageSourceAsset.ALLEGRO)
        "gmail", "email" -> MessageSourceMark("Gmail", MessageSourceAsset.GMAIL)
        "woocommerce" -> MessageSourceMark("WooCommerce", MessageSourceAsset.WOOCOMMERCE)
        else -> MessageSourceMark(
            label = fallbackLabel.trim().take(64).ifBlank { "Nieznane źródło" },
            lightAsset = null,
            darkAsset = null,
        )
    }
}

@Composable
internal fun MessageSourceMarkVisual(
    colors: DlaFlowComposeColors,
    providerId: String,
    fallbackLabel: String,
) {
    val mark = remember(providerId, fallbackLabel) {
        resolveMessageSourceMark(providerId, fallbackLabel)
    }
    val asset = if (colors.dark) mark.darkAsset ?: mark.lightAsset else mark.lightAsset
    val contentDescription = "Źródło: ${mark.label}"
    if (asset == null) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = contentDescription,
            tint = colors.textMuted,
            modifier = Modifier,
        )
    } else {
        Image(
            painter = painterResource(asset.drawableRes()),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = when (asset) {
                MessageSourceAsset.ALLEGRO -> Modifier.width(24.dp).height(12.dp)
                MessageSourceAsset.GMAIL -> Modifier.size(20.dp)
                MessageSourceAsset.WOOCOMMERCE -> Modifier.size(22.dp)
            },
        )
    }
}

private fun MessageSourceAsset.drawableRes(): Int = when (this) {
    MessageSourceAsset.ALLEGRO -> R.drawable.message_source_allegro
    MessageSourceAsset.GMAIL -> R.drawable.message_source_gmail
    MessageSourceAsset.WOOCOMMERCE -> R.drawable.message_source_woocommerce
}
