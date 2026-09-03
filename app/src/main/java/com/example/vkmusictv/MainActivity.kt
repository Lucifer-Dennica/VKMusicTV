package com.example.vkmusictv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/**
 * TV-specific shell for VK's mobile site. Authentication, catalogue and playback are
 * hosted by VK; this Activity adds persistent cookies and an accessible D-pad cursor.
 */
class MainActivity : Activity() {
    companion object {
        private const val VK_AUDIO_URL = "https://m.vk.com/audio"
        private const val CURSOR_HIDE_DELAY_MS = 3500L
    }

    private lateinit var webView: WebView
    private lateinit var cursor: RemoteCursorView
    private lateinit var hint: TextView
    private lateinit var mediaBridge: WebMediaSessionBridge
    private lateinit var keyboard: TvKeyboardView
    private val handler = Handler(Looper.getMainLooper())
    private val hideCursor = Runnable { cursor.hide() }
    private var pageReady = false
    private var keyboardOpen = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(10, 13, 18)) }
        webView = createWebView()
        configureCookies()
        cursor = RemoteCursorView(this)
        hint = createHelpOverlay()

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(cursor, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(hint)
        setContentView(root)

        mediaBridge = WebMediaSessionBridge(this, webView)
        webView.loadUrl(savedInstanceState?.getString("url") ?: VK_AUDIO_URL)
    }

    private fun configureCookies() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.rgb(10, 13, 18))
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString VKMusicTV/1.0"
        }
        addJavascriptInterface(object {
            @JavascriptInterface
            fun onInputFocus(value: String?) {
                runOnUiThread { openKeyboard(value.orEmpty()) }
            }
        }, "TvMusicHost")
        overScrollMode = View.OVER_SCROLL_NEVER
        isFocusable = false
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Keep only normal http(s) navigation inside the signed-in WebView.
                return request.url.scheme !in setOf("http", "https")
            }
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                installInputBridge(view)
                if (!pageReady) {
                    pageReady = true
                    hint.animate().alpha(0f).setStartDelay(4500L).setDuration(600L).withEndAction {
                        hint.visibility = View.GONE
                    }.start()
                }
            }
        }
    }

    private fun installInputBridge(view: WebView) {
        view.evaluateJavascript("""
            (function () {
              if (window.__tvMusicInputBridge) return;
              window.__tvMusicInputBridge = true;
              document.addEventListener('focusin', function (event) {
                var e = event.target;
                if (e && (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA')) {
                  TvMusicHost.onInputFocus(e.value || '');
                }
              }, true);
            })();
        """.trimIndent(), null)
    }

    private fun openKeyboard(initialText: String) {
        if (!::keyboard.isInitialized) {
            keyboard = TvKeyboardView(this,
                onTextChanged = { value ->
                    val escaped = org.json.JSONObject.quote(value)
                    webView.evaluateJavascript("""
                        (function () {
                          var e = document.activeElement;
                          if (e && (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA')) {
                            e.value = $escaped;
                            e.dispatchEvent(new Event('input', {bubbles:true}));
                            e.dispatchEvent(new Event('change', {bubbles:true}));
                          }
                        })();
                    """.trimIndent(), null)
                },
                onDone = { closeKeyboard() },
                onDismiss = { closeKeyboard() }
            )
        }
        keyboard.setInitialText(initialText)
        if (!keyboardOpen) {
            keyboardOpen = true
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(330), Gravity.BOTTOM
            ).apply {
                leftMargin = dp(55)
                rightMargin = dp(55)
                bottomMargin = dp(35)
            }
            (webView.parent as FrameLayout).addView(keyboard, params)
        }
        keyboard.visibility = View.VISIBLE
        cursor.hide()
    }

    private fun closeKeyboard() {
        if (!::keyboard.isInitialized) return
        keyboardOpen = false
        keyboard.visibility = View.GONE
        webView.requestFocus()
    }

    private fun createHelpOverlay(): TextView {
        val chip = TextView(this).apply {
            text = "  VK MUSIC TV     Стрелки — курсор     OK — выбрать     BACK — назад  "
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(225, 20, 27, 38))
                setStroke(dp(1), Color.rgb(55, 82, 115))
            }
            elevation = dp(8).toFloat()
        }
        return chip.also {
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = dp(24) }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::keyboard.isInitialized && keyboardOpen) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val step = if (event.repeatCount > 0) dp(8).toFloat() else dp(18).toFloat()
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> return moveCursor(-step, 0f)
                KeyEvent.KEYCODE_DPAD_RIGHT -> return moveCursor(step, 0f)
                KeyEvent.KEYCODE_DPAD_UP -> return moveCursor(0f, -step)
                KeyEvent.KEYCODE_DPAD_DOWN -> return moveCursor(0f, step)
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (event.repeatCount == 0) tapWebPage()
                    return true
                }
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_SEARCH -> {
                    focusSiteSearch()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) webView.goBack() else finish()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveCursor(dx: Float, dy: Float): Boolean {
        cursor.moveBy(dx, dy)
        cursor.show()
        handler.removeCallbacks(hideCursor)
        handler.postDelayed(hideCursor, CURSOR_HIDE_DELAY_MS)
        return true
    }

    /** Sends a complete touch gesture to the WebView at the current virtual location. */
    private fun tapWebPage() {
        cursor.show()
        handler.removeCallbacks(hideCursor)
        handler.postDelayed(hideCursor, CURSOR_HIDE_DELAY_MS)
        val x = min(cursor.xPosition, (webView.width - 1).toFloat()).coerceAtLeast(0f)
        val y = min(cursor.yPosition, (webView.height - 1).toFloat()).coerceAtLeast(0f)
        val downAt = android.os.SystemClock.uptimeMillis()
        webView.dispatchTouchEvent(MotionEvent.obtain(downAt, downAt, MotionEvent.ACTION_DOWN, x, y, 0))
        webView.dispatchTouchEvent(MotionEvent.obtain(downAt, downAt + 60L, MotionEvent.ACTION_UP, x, y, 0))
    }

    private fun focusSiteSearch() {
        val script = """
            (function () {
                const element = document.querySelector('input[type=search], input[name=q], input[placeholder*=Поиск], input[placeholder*=Search]');
                if (element) { element.focus(); element.scrollIntoView({block: 'center'}); }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
        cursor.show()
        handler.removeCallbacks(hideCursor)
        handler.postDelayed(hideCursor, CURSOR_HIDE_DELAY_MS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("url", webView.url)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        CookieManager.getInstance().flush()
        mediaBridge.release()
        webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
