package pl.dlaflow.mobile

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DlaFlowSessionTransitionOverlay(context: Context) : FrameLayout(context) {
    private val theme = transitionTheme()
    private val loaderView = SessionLoaderView(context)
    private val titleView = label("", 28f, theme.strong, true)
    private val descriptionView = label("", 14f, theme.muted, true).apply {
        gravity = Gravity.CENTER
        maxLines = 3
    }
    private val progressFill = View(context).apply {
        background = rounded(theme.primary, dp(999))
        pivotX = 0f
        scaleX = 0.18f
    }
    private val stepsRow = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
    }
    private var hideRunnable: Runnable? = null

    init {
        visibility = GONE
        alpha = 0f
        isClickable = true
        isFocusable = true
        setBackgroundColor(theme.overlay)

        addView(View(context).apply {
            background = verticalBackground(theme)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        })

        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, dp(24), 0)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)

            addView(ImageView(context).apply {
                setImageResource(if (theme.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(252), dp(58))
            })
            addView(loaderView.apply {
                layoutParams = LinearLayout.LayoutParams(dp(190), dp(190)).withTop(44)
            })
            addView(titleView.apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).withTop(34)
            })
            addView(descriptionView.apply {
                layoutParams = LinearLayout.LayoutParams(dp(360), LayoutParams.WRAP_CONTENT).withTop(12)
            })
            addView(FrameLayout(context).apply {
                background = rounded(theme.track, dp(999))
                clipToOutline = false
                layoutParams = LinearLayout.LayoutParams(dp(360), dp(8)).withTop(28)
                addView(progressFill, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            })
            addView(stepsRow.apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).withTop(18)
            })
        })
    }

    fun show(title: String, description: String, activeStepIndex: Int, progress: Int, steps: List<String>, animateIn: Boolean = true) {
        hideRunnable?.let { removeCallbacks(it) }
        hideRunnable = null
        titleView.text = title
        descriptionView.text = description
        renderSteps(steps, activeStepIndex)
        setProgress(progress)
        visibility = VISIBLE
        loaderView.start()
        animate().cancel()
        if (animateIn) {
            animate().alpha(1f).setDuration(180).start()
        } else {
            alpha = 1f
        }
    }

    fun update(activeStepIndex: Int, progress: Int, steps: List<String>) {
        renderSteps(steps, activeStepIndex)
        setProgress(progress)
    }

    fun finishAndHide(delayMs: Long = 360L) {
        hideRunnable?.let { removeCallbacks(it) }
        val nextRunnable = Runnable {
            animate().alpha(0f).setDuration(260).withEndAction {
                visibility = GONE
                loaderView.stop()
            }.start()
        }
        hideRunnable = nextRunnable
        postDelayed(nextRunnable, delayMs)
    }

    private fun renderSteps(steps: List<String>, activeStepIndex: Int) {
        stepsRow.removeAllViews()
        steps.forEachIndexed { index, step ->
            stepsRow.addView(stepPill(step, index, activeStepIndex))
        }
    }

    private fun stepPill(text: String, index: Int, activeStepIndex: Int): LinearLayout {
        val dotColor = when {
            index < activeStepIndex -> theme.success
            index == activeStepIndex -> theme.primary
            else -> theme.dotIdle
        }
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(theme.surface, dp(18), theme.border, dp(1))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).withRight(6)
            addView(View(context).apply {
                background = rounded(dotColor, dp(999))
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).withRight(8)
            })
            addView(label(text, 12f, theme.muted, true))
        }
    }

    private fun setProgress(progress: Int) {
        val safeProgress = progress.coerceIn(8, 100)
        progressFill.animate().scaleX(safeProgress / 100f).setDuration(300).start()
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(context).apply {
            this.text = text
            includeFontPadding = false
            textSize = size
            setTextColor(color)
            typeface = appTypeface(if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun appTypeface(style: Int): Typeface {
        val base = ResourcesCompat.getFont(context, R.font.inter_variable) ?: Typeface.create("sans-serif", Typeface.NORMAL)
        return Typeface.create(base, style)
    }

    private fun transitionTheme(): TransitionTheme {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (dark) {
            TransitionTheme(
                dark = true,
                overlay = sessionTransitionOverlayColor(dark = true),
                surface = color(0x171C27),
                border = color(0x202735),
                strong = color(0xF8FAFC),
                muted = color(0x9AA7BA),
                primary = color(0x9B83FF),
                accent = color(0x7DB5FF),
                soft = Color.argb(42, 123, 92, 246),
                track = color(0x222938),
                success = color(0x5EEAD4),
                dotIdle = color(0x748198),
            )
        } else {
            TransitionTheme(
                dark = false,
                overlay = sessionTransitionOverlayColor(dark = false),
                surface = color(0xFFFFFF),
                border = color(0xE4E9F2),
                strong = color(0x070A52),
                muted = color(0x64748B),
                primary = color(0x7B5CF6),
                accent = color(0x4F93FF),
                soft = color(0xF1ECFF),
                track = color(0xEDF1F7),
                success = color(0x0B8F78),
                dotIdle = color(0xCFD8E6),
            )
        }
    }

    private fun verticalBackground(theme: TransitionTheme) = android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
        sessionTransitionBackgroundColors(theme.dark),
    )

    private fun rounded(fill: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0) = android.graphics.drawable.GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius.toFloat()
        if (strokeColor != null && strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(value: Long): Int = (0xFF000000L or value).toInt()

    private fun LinearLayout.LayoutParams.withTop(top: Int): LinearLayout.LayoutParams {
        topMargin = dp(top)
        return this
    }

    private fun LinearLayout.LayoutParams.withRight(right: Int): LinearLayout.LayoutParams {
        rightMargin = dp(right)
        return this
    }

    private data class TransitionTheme(
        val dark: Boolean,
        val overlay: Int,
        val surface: Int,
        val border: Int,
        val strong: Int,
        val muted: Int,
        val primary: Int,
        val accent: Int,
        val soft: Int,
        val track: Int,
        val success: Int,
        val dotIdle: Int,
    )
}

internal fun sessionTransitionBackgroundColors(dark: Boolean): IntArray {
    val background = sessionTransitionOverlayColor(dark)
    return intArrayOf(background, background)
}

internal fun sessionTransitionOverlayColor(dark: Boolean): Int {
    return if (dark) {
        0xFF0F131D.toInt()
    } else {
        0xFFF8F9FC.toInt()
    }
}

private class SessionLoaderView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcBounds = RectF()
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 6800L
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }
    private var phase = 0f

    fun start() {
        if (!animator.isStarted) {
            animator.start()
        }
    }

    fun stop() {
        animator.cancel()
        phase = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = size / 2f
        val primary = if (dark) color(0x9B83FF) else color(0x7B5CF6)
        val accent = if (dark) color(0x7DB5FF) else color(0x4F93FF)
        val pale = if (dark) colorWithAlpha(0x9B83FF, 0x2A) else color(0xE7E0FF)
        val subtle = if (dark) colorWithAlpha(0xFFFFFF, 0x18) else color(0xEEF2F8)

        drawCircleStroke(canvas, centerX, centerY, radius - dp(1), dp(1).toFloat(), pale)
        drawCircleStroke(canvas, centerX, centerY, radius - dp(11), dp(1).toFloat(), subtle)
        drawArc(canvas, centerX, centerY, radius - dp(20), dp(2).toFloat(), primary, phase, 118f)
        drawArc(canvas, centerX, centerY, radius - dp(32), dp(1).toFloat(), accent, -phase * 0.78f + 80f, 96f)
        drawArc(canvas, centerX, centerY, radius - dp(45), dp(1).toFloat(), colorWithAlpha(0xBDE7FF, if (dark) 0xAA else 0xCC), phase * 0.52f + 200f, 76f)
        drawArc(canvas, centerX, centerY, radius - dp(58), dp(1).toFloat(), colorWithAlpha(0x7B5CF6, if (dark) 0x80 else 0x70), -phase * 0.95f + 260f, 64f)
        drawCircleStroke(canvas, centerX, centerY, radius - dp(68), dp(1).toFloat(), subtle)

        drawOrbitDot(canvas, centerX, centerY, radius - dp(8), phase * 1.25f - 30f, primary, dp(5).toFloat())
        drawOrbitDot(canvas, centerX, centerY, radius - dp(24), -phase * 1.72f + 85f, accent, dp(4).toFloat())
        drawOrbitDot(canvas, centerX, centerY, radius - dp(41), phase * 0.92f + 180f, color(0xBDE7FF), dp(3).toFloat())
        drawOrbitDot(canvas, centerX, centerY, radius - dp(57), -phase * 1.05f + 270f, color(0xC8B8FF), dp(3).toFloat())

        paint.style = Paint.Style.FILL
        paint.color = if (dark) color(0x171C27) else color(0xFFFFFF)
        canvas.drawCircle(centerX, centerY, dp(37).toFloat(), paint)
        drawCircleStroke(canvas, centerX, centerY, dp(37).toFloat(), dp(1).toFloat(), if (dark) colorWithAlpha(0x9B83FF, 0x59) else color(0xD9CBFF))

        val iconSize = dp(38)
        val left = (centerX - iconSize / 2).toInt()
        val top = (centerY - iconSize / 2).toInt()
        val icon = context.getDrawable(R.drawable.dlaflow_app_icon)
        icon?.setBounds(left, top, left + iconSize, top + iconSize)
        icon?.draw(canvas)
    }

    private fun drawCircleStroke(canvas: Canvas, cx: Float, cy: Float, radius: Float, strokeWidth: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = color
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawArc(canvas: Canvas, cx: Float, cy: Float, radius: Float, strokeWidth: Float, color: Int, start: Float, sweep: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = strokeWidth
        paint.color = color
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcBounds, start, sweep, false, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawOrbitDot(canvas: Canvas, cx: Float, cy: Float, radius: Float, degrees: Float, color: Int, dotRadius: Float) {
        val radians = Math.toRadians(degrees.toDouble())
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(cx + (cos(radians) * radius).toFloat(), cy + (sin(radians) * radius).toFloat(), dotRadius, paint)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(value: Long): Int = (0xFF000000L or value).toInt()

    private fun colorWithAlpha(rgb: Long, alpha: Int): Int = ((alpha.toLong().coerceIn(0, 255) shl 24) or rgb).toInt()
}
