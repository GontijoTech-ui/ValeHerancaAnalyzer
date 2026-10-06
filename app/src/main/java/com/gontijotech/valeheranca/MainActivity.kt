package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private val gameUrl =
        "https://www.astrocade.com/games/vale-da-heran%C3%A7a-renova%C3%A7%C3%A3o/01M46K2HXGZ4F46MRCKS6WTQ6Y"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enterFullscreen()

        webView = WebView(this)
        webView.setBackgroundColor(Color.BLACK)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = true
            allowContentAccess = true

            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)

            mediaPlaybackRequiresUserGesture = false

            useWideViewPort = true
            loadWithOverviewMode = true

            builtInZoomControls = false
            displayZoomControls = false

            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) {
                    aplicarModoSomenteJogo(view)
                }
            }
        }

        setContentView(webView)
        webView.loadUrl(gameUrl)
    }

    /**
     * Injeta CSS e JavaScript para forçar apenas a exibição
     * do jogo e ocultar barras de navegação externas.
     */
    private fun aplicarModoSomenteJogo(view: WebView) {
        val javascript = """
            (function() {
                var estiloId = 'vale_heranca_fullscreen_css';

                if (!document.getElementById(estiloId)) {
                    var style = document.createElement('style');
                    style.id = estiloId;
                    style.innerHTML = `
                        /* Oculta barras de navegacao, menus e rodapes */
                        nav, footer, header,
                        [class*="bottom-bar" i], [class*="navigation" i], 
                        [class*="tab-bar" i], [class*="footer" i],
                        div[class*="social" i], div[class*="actions" i] {
                            display: none !important;
                        }

                        html, body {
                            margin: 0 !important;
                            padding: 0 !important;
                            width: 100vw !important;
                            height: 100vh !important;
                            overflow: hidden !important;
                            background-color: #000 !important;
                        }

                        /* Expande o canvas para ocupar o ecra inteiro */
                        canvas {
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            width: 100vw !important;
                            height: 100vh !important;
                            max-width: 100vw !important;
                            max-height: 100vh !important;
                            z-index: 2147483647 !important;
                            object-fit: contain !important;
                        }
                    `;
                    document.head.appendChild(style);
                }

                function isolarAreaJogo() {
                    var canvases = document.querySelectorAll('canvas');
                    for (var i = 0; i < canvases.length; i++) {
                        var c = canvases[i];
                        var rect = c.getBoundingClientRect();

                        if (rect.width > 100 || rect.height > 100 || c.width > 100) {
                            c.style.setProperty('position', 'fixed', 'important');
                            c.style.setProperty('top', '0px', 'important');
                            c.style.setProperty('left', '0px', 'important');
                            c.style.setProperty('width', '100vw', 'important');
                            c.style.setProperty('height', '100vh', 'important');
                            c.style.setProperty('z-index', '2147483647', 'important');
                            c.style.setProperty('object-fit', 'contain', 'important');
                            return true;
                        }
                    }
                    return false;
                }

                if (isolarAreaJogo()) return;

                var tentativas = 0;
                var timer = setInterval(function() {
                    tentativas++;
                    if (isolarAreaJogo() || tentativas > 30) {
                        clearInterval(timer);
                    }
                }, 500);
            })();
        """.trimIndent()

        view.post {
            view.evaluateJavascript(javascript, null)
        }
    }

    private fun enterFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
        enterFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterFullscreen()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
