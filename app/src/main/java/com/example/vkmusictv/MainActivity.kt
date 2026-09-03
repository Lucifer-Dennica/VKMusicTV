package com.example.vkmusictv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/** Native TV navigation shell; WebView is used only for VK-hosted flows. */
class MainActivity : Activity() {
    companion object { private const val VK_AUDIO_URL = "https://m.vk.com/audio" }

    private lateinit var root: FrameLayout
    private lateinit var dashboard: DashboardView
    private lateinit var webView: WebView
    private lateinit var keyboard: TvKeyboardView
    private lateinit var mediaBridge: WebMediaSessionBridge
    private var keyboardOpen = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(9, 12, 18)) }
        dashboard = DashboardView(this, ::handleDashboardAction)
        webView = createWebView()
        configureCookies()
        webView.visibility = View.GONE
        root.addView(dashboard, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        mediaBridge = WebMediaSessionBridge(this, webView)
    }

    private fun handleDashboardAction(action: DashboardView.Action) {
        when (action) {
            DashboardView.Action.MUSIC, DashboardView.Action.PLAYLISTS, DashboardView.Action.FAVOURITES, DashboardView.Action.LOGIN -> openVk()
            DashboardView.Action.SEARCH -> { openVk(); webView.postDelayed({ focusSearch() }, 800L) }
        }
    }

    private fun openVk() {
        dashboard.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.requestFocus()
        if (webView.url == null) webView.loadUrl(VK_AUDIO_URL)
    }

    private fun showDashboard() {
        closeKeyboard()
        webView.visibility = View.GONE
        dashboard.visibility = View.VISIBLE
        dashboard.requestFocus()
    }

    private fun configureCookies() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) setAcceptThirdPartyCookies(webView, true)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.rgb(9, 12, 18))
        isFocusable = true
        isFocusableInTouchMode = true
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString VKMusicTV/2.0"
        }
        addJavascriptInterface(object {
            @JavascriptInterface fun onInputFocus(value: String?) { runOnUiThread { openKeyboard(value.orEmpty()) } }
        }, "TvMusicHost")
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.url.scheme !in setOf("http", "https")
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                installInputBridge(view)
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
                if (e && (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA')) TvMusicHost.onInputFocus(e.value || '');
              }, true);
            })();
        """.trimIndent(), null)
    }

    private fun focusSearch() {
        webView.evaluateJavascript("""
            (function () {
              var e = document.querySelector('input[type=search],input[name=q],input[placeholder*=Поиск],input[placeholder*=Search]);
              if (e) { e.focus(); e.scrollIntoView({block:'center'}); }
            })();
        """.trimIndent(), null)
    }

    private fun openKeyboard(initialText: String) {
        if (!::keyboard.isInitialized) {
            keyboard = TvKeyboardView(this,
                onTextChanged = { value ->
                    val escaped = org.json.JSONObject.quote(value)
                    webView.evaluateJavascript("""(function(){var e=document.activeElement;if(e&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA')){e.value=$escaped;e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}})();""", null)
                },
                onDone = ::closeKeyboard,
                onDismiss = ::closeKeyboard
            )
        }
        keyboard.setInitialText(initialText)
        if (!keyboardOpen) {
            keyboardOpen = true
            root.addView(keyboard, FrameLayout.LayoutParams(-1, dp(330)).apply { leftMargin = dp(55); rightMargin = dp(55); bottomMargin = dp(35); gravity = android.view.Gravity.BOTTOM })
        }
        keyboard.visibility = View.VISIBLE
    }

    private fun closeKeyboard() {
        if (!::keyboard.isInitialized) return
        keyboardOpen = false
        keyboard.visibility = View.GONE
        webView.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (keyboardOpen) { closeKeyboard(); return true }
            if (webView.visibility == View.VISIBLE) { if (webView.canGoBack()) webView.goBack() else showDashboard(); return true }
        }
        if (keyboardOpen) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN && webView.visibility == View.VISIBLE) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> { moveWebFocus(-1); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> { moveWebFocus(1); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { clickWebFocus(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveWebFocus(delta: Int) {
        webView.evaluateJavascript("""
            (function () {
              var items = Array.from(document.querySelectorAll('a,button,input,textarea,[role=button],[tabindex]:not([tabindex="-1"])')).filter(function(e) {
                var r=e.getBoundingClientRect(), s=getComputedStyle(e);
                return r.width>0 && r.height>0 && s.visibility!=='hidden' && s.display!=='none';
              });
              if (!items.length) return;
              var i = items.indexOf(document.activeElement);
              i = i < 0 ? (delta > 0 ? 0 : items.length - 1) : Math.max(0, Math.min(items.length - 1, i + $delta));
              items[i].focus(); items[i].scrollIntoView({block:'center', inline:'nearest'});
            })();
        """.trimIndent(), null)
    }

    private fun clickWebFocus() {
        webView.evaluateJavascript("""
            (function () { var e=document.activeElement; if(e && e !== document.body) e.click(); })();
        """.trimIndent(), null)
    }

    override fun onResume() { super.onResume(); if (::webView.isInitialized) webView.onResume() }
    override fun onDestroy() {
        if (::webView.isInitialized) { CookieManager.getInstance().flush(); mediaBridge.release(); webView.destroy() }
        super.onDestroy()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
