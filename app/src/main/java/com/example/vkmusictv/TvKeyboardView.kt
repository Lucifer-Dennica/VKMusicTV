package com.example.vkmusictv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** A simple focusable keyboard designed for D-pad navigation on Android TV. */
class TvKeyboardView(
    context: Context,
    private val onTextChanged: (String) -> Unit,
    private val onDone: () -> Unit,
    private val onDismiss: () -> Unit
) : LinearLayout(context) {
    private val display = TextView(context)
    private val input = StringBuilder()
    private var russian = false
    private val rows = mutableListOf<LinearLayout>()
    private val blue = Color.rgb(103, 168, 255)
    private val panel = Color.rgb(20, 27, 38)

    init {
        orientation = VERTICAL
        setPadding(dp(18), dp(14), dp(18), dp(16))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Color.argb(248, panel.red(), panel.green(), panel.blue()))
            setStroke(dp(1), Color.rgb(67, 94, 130))
        }
        elevation = dp(18).toFloat()
        isFocusable = true
        isFocusableInTouchMode = true
        build()
    }

    private fun build() {
        display.apply {
            text = ""
            hint = "Введите текст"
            setHintTextColor(Color.rgb(154, 170, 192))
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.rgb(10, 13, 18))
            }
        }
        addView(display, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        addRow(if (russian) listOf("Й", "Ц", "У", "К", "Е", "Н", "Г", "Ш", "Щ", "З") else listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"))
        addRow(if (russian) listOf("Ф", "Ы", "В", "А", "П", "Р", "О", "Л", "Д", "Ж") else listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"))
        addRow(if (russian) listOf("Я", "Ч", "С", "М", "И", "Т", "Ь", "Б", "Ю") else listOf("Z", "X", "C", "V", "B", "N", "M"))
        val actions = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        addButton(actions, "Рус/Eng", 1.7f) { russian = !russian; rebuildKeys() }
        addButton(actions, "ПРОБЕЛ", 3.2f) { insert(" ") }
        addButton(actions, "⌫", 1.1f) { if (input.isNotEmpty()) { input.deleteCharAt(input.lastIndex); publish() } }
        addButton(actions, "Готово", 1.8f) { onDone() }
        addButton(actions, "×", 0.9f) { onDismiss() }
        addView(actions, LayoutParams(LayoutParams.MATCH_PARENT, dp(54)))
        post { requestFocus() }
    }

    private fun addRow(keys: List<String>) {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        keys.forEach { key -> addButton(row, key, 1f) { insert(key) } }
        rows.add(row)
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
    }

    private fun addButton(row: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        val button = Button(context).apply {
            text = label
            textSize = if (label.length > 4) 12f else 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            isFocusable = true
            isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.rgb(39, 51, 68))
                setStroke(dp(1), Color.rgb(66, 86, 112))
            }
            setOnFocusChangeListener { view, focused ->
                if (focused) (view.background as GradientDrawable).setColor(Color.rgb(35, 111, 207))
                else (view.background as GradientDrawable).setColor(Color.rgb(39, 51, 68))
            }
            setOnClickListener { action() }
        }
        val params = LayoutParams(0, dp(44), weight).apply {
            setMargins(dp(3), dp(2), dp(3), dp(2))
        }
        row.addView(button, params)
    }

    private fun rebuildKeys() {
        rows.forEach { removeView(it) }
        rows.clear()
        val anchor = indexOfChild(display) + 1
        val keyRows = listOf(
            if (russian) listOf("Й", "Ц", "У", "К", "Е", "Н", "Г", "Ш", "Щ", "З") else listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            if (russian) listOf("Ф", "Ы", "В", "А", "П", "Р", "О", "Л", "Д", "Ж") else listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            if (russian) listOf("Я", "Ч", "С", "М", "И", "Т", "Ь", "Б", "Ю") else listOf("Z", "X", "C", "V", "B", "N", "M")
        )
        keyRows.forEachIndexed { offset, keys ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            keys.forEach { key -> addButton(row, key, 1f) { insert(key) } }
            rows.add(row)
            addView(row, indexOfChild(display) + 1 + offset, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        }
    }

    fun setInitialText(value: String) {
        input.clear(); input.append(value); publish()
    }

    private fun insert(value: String) { input.append(value); publish() }
    private fun publish() {
        display.text = input.toString()
        onTextChanged(input.toString())
    }
    private fun Int.red() = this shr 16 and 0xFF
    private fun Int.green() = this shr 8 and 0xFF
    private fun Int.blue() = this and 0xFF
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
