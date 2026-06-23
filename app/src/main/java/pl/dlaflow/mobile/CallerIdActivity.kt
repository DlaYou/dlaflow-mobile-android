package pl.dlaflow.mobile

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

class CallerIdActivity : Activity() {
    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable { finish() }
    private var phoneStateReceiverRegistered = false
    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                return
            }

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        renderFromIntent(intent)
        registerPhoneStateReceiver()
        scheduleAutoClose()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderFromIntent(intent)
        scheduleAutoClose()
    }

    override fun onDestroy() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        unregisterPhoneStateReceiver()
        super.onDestroy()
    }

    private fun renderFromIntent(intent: Intent) {
        val lookup = intent.getSerializableExtraCompat<CallerIdPayload>(extraPayload)
        setContentView(renderContent(lookup))
    }

    private fun scheduleAutoClose() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        autoCloseHandler.postDelayed(autoCloseRunnable, autoCloseMillis)
    }

    private fun registerPhoneStateReceiver() {
        if (phoneStateReceiverRegistered) {
            return
        }

        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(phoneStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(phoneStateReceiver, filter)
        }
        phoneStateReceiverRegistered = true
    }

    private fun unregisterPhoneStateReceiver() {
        if (!phoneStateReceiverRegistered) {
            return
        }

        runCatching { unregisterReceiver(phoneStateReceiver) }
        phoneStateReceiverRegistered = false
    }

    private fun renderContent(payload: CallerIdPayload?): View {
        val dark = isDarkMode()
        val surface = if (dark) 0x171C27L else 0xFFFFFFL
        val border = if (dark) 0x202735L else 0xEDF0F5L
        val text = if (dark) 0xF8FAFCL else 0x0F172AL
        val muted = if (dark) 0x9AA7BAL else 0x64748BL
        val softPrimary = if (dark) 0x2B2351L else 0xF1ECFFL
        val primary = if (dark) 0xC8B8FFL else 0x4F1BD8L

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(14), dp(18))
            setBackgroundColor(Color.TRANSPARENT)
            addView(LinearLayout(this@CallerIdActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(color(surface), dp(7), color(border), dp(1))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                renderCard(this, payload, text, muted, softPrimary, primary, dark)
            })
        }
    }

    private fun renderCard(
        card: LinearLayout,
        payload: CallerIdPayload?,
        text: Long,
        muted: Long,
        softPrimary: Long,
        primary: Long,
        dark: Boolean,
    ) {
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(ImageView(this).apply {
            setImageResource(resources.getIdentifier("dlaflow_app_icon", "drawable", packageName))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = rounded(color(softPrimary), dp(7), color(if (dark) 0x5946A0 else 0xE4DCFF), dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
        })
        header.addView(label("DlaFlow", 13f, text, true, 0).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withLeft(8)
        })
        header.addView(label("Dzwoni klient", 12f, primary, true, 0).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            background = rounded(color(softPrimary), dp(6), color(if (dark) 0x5946A0 else 0xD8C8FF), dp(1))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(26))
        })
        card.addView(header)
        card.addView(separator(if (dark) 0x2C3546 else 0xE2E8F0))

        if (payload == null || payload.order == null) {
            card.addView(label("Nie znaleziono zamówienia", 18f, text, true, 0))
            card.addView(label(payload?.phone ?: "Numer nieznany", 13f, muted, false, 6))
            return
        }

        val order = payload.order
        card.addView(label(payload.displayName.ifBlank { payload.phone }, 20f, text, true, 0))

        val orderMeta = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(4)
        }
        orderMeta.addView(orderCountBadge(payload.orderCount, dark))
        orderMeta.addView(label(orderCountLabel(payload.orderCount), 13f, text, true, 0).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withLeft(8)
        })
        card.addView(orderMeta)
        card.addView(twoColumn("#${order.orderNumber}", shortClock(order.lastEventAt), text, muted, top = 8))

        val commerce = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(8)
        }
        commerce.addView(statusBadge(order.status))
        commerce.addView(label(order.productSummary.ifBlank { "Produkt" }, 12f, text, true, 0).apply {
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withLeft(8)
        })
        commerce.addView(label(formatAmount(order.amount, order.currency), 12f, text, true, 0).apply {
            gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).withLeft(8)
        })
        card.addView(commerce)
        card.addView(twoColumn("${order.paymentStatus.ifBlank { "Płatność" }}, ${order.delivery.ifBlank { "dostawa" }}", "Ostatnie: ${shortDate(order.sourceCreatedAt)}", text, muted, top = 8))
    }

    private fun twoColumn(leftValue: String, rightValue: String, text: Long, muted: Long, top: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(top)
            addView(label(leftValue, 12f, text, true, 0).apply {
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(label(rightValue, 12f, muted, true, 0).apply {
                gravity = Gravity.RIGHT
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun separator(color: Long): View {
        return View(this).apply {
            setBackgroundColor(color(color))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).withTop(10).withBottom(10)
        }
    }

    private fun statusBadge(text: String): TextView {
        return label(text.ifBlank { "Status" }, 10f, 0x9A3412, true, 0).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            background = rounded(color(0xFEF3C7), dp(5))
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(24))
        }
    }

    private fun orderCountBadge(count: Int, dark: Boolean): TextView {
        return label(count.coerceAtLeast(1).toString(), 11f, if (dark) 0xB8C2D6 else 0x64748B, true, 0).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.TRANSPARENT, dp(14), color(if (dark) 0x334155 else 0xCBD5E1), dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
        }
    }

    private fun label(text: String, size: Float, color: Long, bold: Boolean, top: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color(color))
            includeFontPadding = false
            if (bold) {
                typeface = appTypeface(Typeface.BOLD)
            } else {
                typeface = appTypeface(Typeface.NORMAL)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(top)
        }
    }

    private fun appTypeface(style: Int): Typeface {
        val base = ResourcesCompat.getFont(this, resources.getIdentifier("inter_variable", "font", packageName))
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        return Typeface.create(base, style)
    }

    private fun rounded(fill: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeColor != null && strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun formatAmount(amount: Double, currency: String): String {
        return "${String.format("%.2f", amount).replace(".", ",")} ${currency.ifBlank { "PLN" }}"
    }

    private fun shortDate(value: String): String {
        if (value.length < 10) {
            return "-"
        }

        return "${value.substring(8, 10)}.${value.substring(5, 7)}"
    }

    private fun shortClock(value: String): String {
        if (value.length < 16) {
            return "Dzis"
        }

        return "Dzis ${value.substring(11, 16)}"
    }

    private fun orderCountLabel(count: Int): String {
        return if (count == 1) "Zamówienie" else "Zamówienia"
    }

    private fun LinearLayout.LayoutParams.withTop(top: Int): LinearLayout.LayoutParams {
        topMargin = dp(top)
        return this
    }

    private fun LinearLayout.LayoutParams.withBottom(bottom: Int): LinearLayout.LayoutParams {
        bottomMargin = dp(bottom)
        return this
    }

    private fun LinearLayout.LayoutParams.withLeft(left: Int): LinearLayout.LayoutParams {
        leftMargin = dp(left)
        return this
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun color(value: Long): Int {
        return (0xFF000000L or value).toInt()
    }

    companion object {
        private const val autoCloseMillis = 45_000L
        private const val extraPayload = "pl.dlaflow.mobile.CALLER_ID_PAYLOAD"

        fun createIntent(context: Context, lookup: MobileCallerIdLookup): Intent {
            return Intent(context, CallerIdActivity::class.java)
                .putExtra(extraPayload, CallerIdPayload.fromLookup(lookup))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

data class CallerIdPayload(
    val displayName: String,
    val order: CallerIdOrderPayload?,
    val orderCount: Int,
    val phone: String,
) : java.io.Serializable {
    companion object {
        fun fromLookup(lookup: MobileCallerIdLookup): CallerIdPayload {
            return CallerIdPayload(
                displayName = lookup.displayName,
                order = lookup.primaryOrder?.let {
                    CallerIdOrderPayload(
                        amount = it.amount,
                        currency = it.currency,
                        delivery = it.delivery,
                        lastEventAt = it.lastEventAt,
                        orderNumber = it.orderNumber,
                        paymentStatus = it.paymentStatus,
                        productSummary = it.productSummary,
                        sourceCreatedAt = it.sourceCreatedAt,
                        status = it.status,
                    )
                },
                orderCount = lookup.orderCount,
                phone = lookup.phone,
            )
        }
    }
}

data class CallerIdOrderPayload(
    val amount: Double,
    val currency: String,
    val delivery: String,
    val lastEventAt: String,
    val orderNumber: String,
    val paymentStatus: String,
    val productSummary: String,
    val sourceCreatedAt: String,
    val status: String,
) : java.io.Serializable

private inline fun <reified T : java.io.Serializable> Intent.getSerializableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(name) as? T
    }
}
