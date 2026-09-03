package com.example.vkmusictv

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.Build
import android.view.KeyEvent
import android.webkit.WebView

/**
 * Owns a platform MediaSession so standard headset and remote transport keys reach
 * the embedded site. The player itself remains VK's authenticated HTML5 player.
 */
class WebMediaSessionBridge(
    context: Context,
    private val webView: WebView
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mediaSession = MediaSession(context, "VKMusicTV-WebPlayback")

    init {
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = activateAndRun("play")
            override fun onPause() = runPlayerAction("pause")
            override fun onStop() = runPlayerAction("pause")
            override fun onSkipToNext() = runPlayerAction("next")
            override fun onSkipToPrevious() = runPlayerAction("previous")
            override fun onMediaButtonEvent(mediaButtonEvent: android.content.Intent): Boolean {
                val event = mediaButtonEvent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                return event?.let { handleKey(it) } ?: false
            }
        })
        mediaSession.isActive = true
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun activateAndRun(action: String) {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        runPlayerAction(action)
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        return when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> { activateAndRun("play"); true }
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> { runPlayerAction("pause"); true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> { runPlayerAction("toggle"); true }
            KeyEvent.KEYCODE_MEDIA_NEXT -> { runPlayerAction("next"); true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { runPlayerAction("previous"); true }
            else -> false
        }
    }

    /**
     * VK's DOM changes often. This uses several semantic selectors and is deliberately
     * best-effort; normal D-pad clicking remains the primary control path.
     */
    private fun runPlayerAction(action: String) {
        val script = """
            (function () {
              const all = Array.from(document.querySelectorAll('button,a,[role=button]'));
              const words = {
                play: ['play','играть','слушать','воспроизвести'],
                pause: ['pause','пауза','остановить'],
                toggle: ['play','pause','играть','пауза','слушать'],
                next: ['next','следующ'],
                previous: ['previous','предыдущ']
              }['$action'];
              const node = all.find(function (e) {
                const t = ((e.getAttribute('aria-label') || '') + ' ' +
                  (e.getAttribute('title') || '') + ' ' + (e.className || '')).toLowerCase();
                return words.some(function (w) { return t.indexOf(w) >= 0; });
              });
              if (node) { node.click(); return true; }
              return false;
            })();
        """.trimIndent()
        webView.post { webView.evaluateJavascript(script, null) }
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(null)
    }
}
