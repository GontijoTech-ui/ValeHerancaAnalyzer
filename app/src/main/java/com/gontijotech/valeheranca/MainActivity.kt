package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.app.Activity
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

        /*
         * Tela cheia.
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        /*
         * Mantém a tela ligada enquanto o jogo estiver aberto.
         */
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        /*
         * Modo imersivo:
         *
         * - esconde barra de navegação
         * - esconde barra de status
         * - ocupa a tela inteira
         */
        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        /*
         * Cria somente a WebView.
         */
        webView = WebView(this)

        webView.setBackgroundColor(
            android.graphics.Color.BLACK
        )

        /*
         * Configura o navegador do jogo.
         */
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

            loadWithOverviewMode = false

            cacheMode = WebSettings.LOAD_DEFAULT

            /*
             * Mantém o navegador com aparência compatível
             * com um navegador Android normal.
             */
            userAgentString = userAgentString
        }

        /*
         * Impede que links sejam enviados para outro navegador.
         * Tudo continua dentro desta WebView.
         */
        webView.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {

                    if (!url.isNullOrBlank()) {

                        view?.loadUrl(url)
                    }

                    return true
                }
            }

        /*
         * A WebView é a única coisa exibida na tela.
         */
        setContentView(webView)

        /*
         * Carrega o jogo.
         */
        webView.loadUrl(gameUrl)
    }

    override fun onResume() {
        super.onResume()

        /*
         * Mantém a WebView ativa quando o aplicativo retorna.
         */
        if (::webView.isInitialized) {
            webView.onResume()
        }

        enterFullscreen()
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {

        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
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

        /*
         * Se o jogo tiver histórico de navegação,
         * o botão voltar volta dentro do próprio jogo.
         *
         * Caso contrário, fecha o aplicativo.
         */
        if (::webView.isInitialized && webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}


