package com.example.vkmusictv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.View
import kotlin.math.min

/** Native, focus-driven home screen. No pointer or touch input is needed here. */
class DashboardView(context: Context, private val onAction: (Action) -> Unit) : View(context) {
    enum class Action { MUSIC, SEARCH, PLAYLISTS, FAVOURITES, LOGIN }

    private val density = resources.displayMetrics.density
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(9, 12, 18) }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(21, 29, 41) }
    private val selected = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(32, 105, 196) }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = dp(31f); typeface = Typeface.DEFAULT_BOLD
    }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(164, 180, 201); textSize = dp(16f) }
    private val cardTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = dp(17f); typeface = Typeface.DEFAULT_BOLD
    }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(146, 198, 255); textSize = dp(27f); typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private var focus = 0
    private val labels = listOf("Моя музыка", "Поиск", "Плейлисты", "Избранное", "Войти в VK")
    private val icons = listOf("♫", "⌕", "≡", "♡", "→")

    init { isFocusable = true; isFocusableInTouchMode = true; post { requestFocus() } }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        val w = width.toFloat(); val h = height.toFloat()
        val left = dp(66f); val top = dp(58f)
        canvas.drawText("VK MUSIC", left, top, title)
        canvas.drawText("TV", left + dp(170f), top, title.apply { color = Color.rgb(103, 168, 255) })
        title.color = Color.WHITE
        canvas.drawText("Музыка на большом экране", left, top + dp(31f), body)

        val banner = RectF(left, top + dp(65f), w - left, top + dp(145f))
        card.color = Color.rgb(18, 47, 83); canvas.drawRoundRect(banner, dp(14f), dp(14f), card)
        card.color = Color.rgb(21, 29, 41)
        canvas.drawText("Слушайте свою музыку", left + dp(25f), banner.top + dp(33f), cardTitle)
        canvas.drawText("Стрелки — выбор раздела     OK — открыть", left + dp(25f), banner.top + dp(59f), body)

        val gap = dp(14f); val startY = top + dp(170f); val cardW = (w - 2 * left - 2 * gap) / 3f
        for (i in 0..2) drawCard(canvas, i, RectF(left + i * (cardW + gap), startY, left + i * (cardW + gap) + cardW, startY + dp(126f)))
        val row2Y = startY + dp(143f)
        drawCard(canvas, 3, RectF(left, row2Y, left + cardW, row2Y + dp(112f)))
        drawCard(canvas, 4, RectF(left + cardW + gap, row2Y, left + 2 * cardW + gap, row2Y + dp(112f)))

        val footer = h - dp(34f)
        canvas.drawText("ВК открывается в защищённом окне приложения", left, footer, body)
        canvas.drawText("D-PAD", w - left - dp(55f), footer, body)
    }

    private fun drawCard(canvas: Canvas, index: Int, rect: RectF) {
        val paint = if (focus == index) selected else card
        canvas.drawRoundRect(rect, dp(13f), dp(13f), paint)
        canvas.drawText(icons[index], rect.centerX(), rect.top + dp(45f), icon)
        canvas.drawText(labels[index], rect.left + dp(18f), rect.bottom - dp(23f), cardTitle)
        if (focus == index) {
            val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(142, 205, 255); style = Paint.Style.STROKE; strokeWidth = dp(2f) }
            canvas.drawRoundRect(RectF(rect.left + dp(2f), rect.top + dp(2f), rect.right - dp(2f), rect.bottom - dp(2f)), dp(12f), dp(12f), outline)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val next = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> when (focus) { 1, 2, 4 -> focus - 1; else -> focus }
            KeyEvent.KEYCODE_DPAD_RIGHT -> when (focus) { 0, 1, 3 -> focus + 1; else -> focus }
            KeyEvent.KEYCODE_DPAD_UP -> when (focus) { 3, 4 -> focus - 3; else -> focus }
            KeyEvent.KEYCODE_DPAD_DOWN -> when (focus) { 0, 1, 2 -> focus + 3; else -> focus }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { onAction(Action.values()[focus]); return true }
            else -> return super.onKeyDown(keyCode, event)
        }
        if (next != focus) { focus = min(4, next); invalidate() }
        return true
    }

    private fun dp(value: Float): Float = value * density
}
