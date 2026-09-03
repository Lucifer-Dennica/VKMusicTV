package com.example.vkmusictv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Large, high-contrast cursor drawn above the WebView. It receives no focus; all
 * remote events remain at the Activity and are converted into WebView touch events.
 */
class RemoteCursorView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 8, 12, 18)
        style = Paint.Style.FILL
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(103, 168, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 0, 0)
        setShadowLayer(8f * density, 0f, 2f * density, Color.BLACK)
    }

    var xPosition = 0f
        private set
    var yPosition = 0f
        private set

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, shadow)
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0f
    }

    fun placeInCenter() {
        xPosition = width / 2f
        yPosition = height / 2f
        invalidate()
    }

    fun moveBy(dx: Float, dy: Float) {
        xPosition = min(max(0f, xPosition + dx), width.toFloat())
        yPosition = min(max(0f, yPosition + dy), height.toFloat())
        invalidate()
    }

    fun show() {
        animate().cancel()
        animate().alpha(1f).setDuration(90L).start()
    }

    fun hide() {
        animate().cancel()
        animate().alpha(0f).setDuration(320L).start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (oldw == 0 || oldh == 0) placeInCenter()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = 20f * density
        canvas.drawCircle(xPosition, yPosition + 2f * density, radius + 4f * density, shadow)
        canvas.drawCircle(xPosition, yPosition, radius, outer)
        canvas.drawCircle(xPosition, yPosition, radius, ring)
        canvas.drawCircle(xPosition, yPosition, 4f * density, dot)

        val line = 10f * density
        canvas.drawRoundRect(RectF(xPosition - line, yPosition - density, xPosition + line, yPosition + density), density, density, ring)
        canvas.drawRoundRect(RectF(xPosition - density, yPosition - line, xPosition + density, yPosition + line), density, density, ring)
    }
}
