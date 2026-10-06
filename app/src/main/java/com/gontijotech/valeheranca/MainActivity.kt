package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())

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
            loadWithOverviewMode = false

            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                if (view == null) return

                /*
                 * O jogo da Astrocade pode ser criado
                 * alguns segundos depois do carregamento
                 * inicial da página.
                 *
                 * Por isso fazemos várias tentativas.
                 */
                aplicarModoSomenteJogo(view)
            }
        }

        setContentView(webView)

        webView.loadUrl(gameUrl)
    }

    /**
     * Injeta o código que procura o elemento
     * responsável pelo jogo.
     */
    private fun aplicarModoSomenteJogo(view: WebView) {

        val javascript = """
            (function() {

                if (window.__GTSTORE_GAME_MODE__) {
                    return;
                }

                window.__GTSTORE_GAME_MODE__ = true;

                function esconderTudoExceto(elemento) {

                    if (!elemento) {
                        return false;
                    }

                    var body = document.body;

                    if (!body) {
                        return false;
                    }

                    /*
                     * Caminho entre o elemento do jogo
                     * e o BODY.
                     */
                    var caminho = [];

                    var atual = elemento;

                    while (atual && atual !== body) {
                        caminho.push(atual);
                        atual = atual.parentElement;
                    }

                    caminho.push(body);

                    /*
                     * Esconde elementos que não fazem
                     * parte da árvore do jogo.
                     */
                    var todos = body.querySelectorAll("*");

                    for (var i = 0; i < todos.length; i++) {

                        var el = todos[i];

                        if (caminho.indexOf(el) === -1) {

                            el.style.setProperty(
                                "display",
                                "none",
                                "important"
                            );

                        }
                    }

                    /*
                     * Limpa os containers que ficam
                     * no caminho até o jogo.
                     */
                    for (var j = 0; j < caminho.length; j++) {

                        var pai = caminho[j];

                        pai.style.setProperty(
                            "margin",
                            "0",
                            "important"
                        );

                        pai.style.setProperty(
                            "padding",
                            "0",
                            "important"
                        );

                        pai.style.setProperty(
                            "border",
                            "0",
                            "important"
                        );

                        pai.style.setProperty(
                            "background",
                            "transparent",
                            "important"
                        );

                        pai.style.setProperty(
                            "box-shadow",
                            "none",
                            "important"
                        );
                    }

                    /*
                     * Corpo inteiro vira uma tela preta.
                     */
                    body.style.setProperty(
                        "margin",
                        "0",
                        "important"
                    );

                    body.style.setProperty(
                        "padding",
                        "0",
                        "important"
                    );

                    body.style.setProperty(
                        "width",
                        "100vw",
                        "important"
                    );

                    body.style.setProperty(
                        "height",
                        "100vh",
                        "important"
                    );

                    body.style.setProperty(
                        "overflow",
                        "hidden",
                        "important"
                    );

                    body.style.setProperty(
                        "background",
                        "#000",
                        "important"
                    );

                    /*
                     * Coloca o jogo em tela cheia.
                     */
                    elemento.style.setProperty(
                        "position",
                        "fixed",
                        "important"
                    );

                    elemento.style.setProperty(
                        "left",
                        "0",
                        "important"
                    );

                    elemento.style.setProperty(
                        "top",
                        "0",
                        "important"
                    );

                    elemento.style.setProperty(
                        "width",
                        "100vw",
                        "important"
                    );

                    elemento.style.setProperty(
                        "height",
                        "100vh",
                        "important"
                    );

                    elemento.style.setProperty(
                        "max-width",
                        "none",
                        "important"
                    );

                    elemento.style.setProperty(
                        "max-height",
                        "none",
                        "important"
                    );

                    elemento.style.setProperty(
                        "margin",
                        "0",
                        "important"
                    );

                    elemento.style.setProperty(
                        "padding",
                        "0",
                        "important"
                    );

                    elemento.style.setProperty(
                        "border",
                        "0",
                        "important"
                    );

                    elemento.style.setProperty(
                        "z-index",
                        "2147483647",
                        "important"
                    );

                    elemento.style.setProperty(
                        "background",
                        "#000",
                        "important"
                    );

                    elemento.style.setProperty(
                        "overflow",
                        "hidden",
                        "important"
                    );

                    /*
                     * Canvas preserva a proporção do jogo.
                     */
                    if (
                        elemento.tagName &&
                        elemento.tagName.toLowerCase() === "canvas"
                    ) {

                        elemento.style.setProperty(
                            "object-fit",
                            "contain",
                            "important"
                        );
                    }

                    return true;
                }


                function encontrarJogo() {

                    var candidatos = [];

                    /*
                     * Canvas.
                     */
                    var canvases =
                        document.querySelectorAll("canvas");

                    for (var i = 0; i < canvases.length; i++) {

                        candidatos.push({
                            elemento: canvases[i],
                            prioridade: 100
                        });
                    }

                    /*
                     * Iframes.
                     */
                    var iframes =
                        document.querySelectorAll("iframe");

                    for (var j = 0; j < iframes.length; j++) {

                        candidatos.push({
                            elemento: iframes[j],
                            prioridade: 95
                        });
                    }

                    /*
                     * Elementos com nomes comuns
                     * de engines de jogos.
                     */
                    var seletores = [
                        '[id*="game" i]',
                        '[class*="game" i]',
                        '[id*="game-container" i]',
                        '[class*="game-container" i]',
                        '[id*="game-container" i]',
                        '[class*="game-container" i]',
                        '[id*="canvas" i]',
                        '[class*="canvas" i]',
                        '[id*="play" i]',
                        '[class*="play" i]',
                        '[id*="unity" i]',
                        '[class*="unity" i]',
                        '[id*="godot" i]',
                        '[class*="godot" i]',
                        '[id*="phaser" i]',
                        '[class*="phaser" i]',
                        '[id*="pixi" i]',
                        '[class*="pixi" i]'
                    ];

                    for (var s = 0; s < seletores.length; s++) {

                        var encontrados =
                            document.querySelectorAll(
                                seletores[s]
                            );

                        for (
                            var e = 0;
                            e < encontrados.length;
                            e++
                        ) {

                            candidatos.push({
                                elemento: encontrados[e],
                                prioridade: 70
                            });
                        }
                    }

                    /*
                     * Avalia o tamanho visual.
                     */
                    var melhor = null;
                    var melhorPontuacao = 0;

                    for (
                        var c = 0;
                        c < candidatos.length;
                        c++
                    ) {

                        var candidato =
                            candidatos[c].elemento;

                        if (!candidato) {
                            continue;
                        }

                        var rect =
                            candidato.getBoundingClientRect();

                        if (
                            rect.width <= 10 ||
                            rect.height <= 10
                        ) {
                            continue;
                        }

                        var estilo =
                            window.getComputedStyle(
                                candidato
                            );

                        if (
                            estilo.display === "none" ||
                            estilo.visibility === "hidden" ||
                            parseFloat(estilo.opacity) === 0
                        ) {
                            continue;
                        }

                        var area =
                            rect.width * rect.height;

                        var viewportArea =
                            window.innerWidth *
                            window.innerHeight;

                        var proporcao =
                            viewportArea > 0
                                ? area / viewportArea
                                : 0;

                        /*
                         * Quanto maior o elemento,
                         * maior a pontuação.
                         */
                        var pontuacao =
                            candidato === candidatos[c].elemento
                                ? candidatos[c].prioridade
                                : 0;

                        pontuacao +=
                            Math.min(proporcao * 100, 100);

                        /*
                         * Prefere elementos realmente grandes.
                         */
                        if (proporcao > 0.20) {
                            pontuacao += 30;
                        }

                        if (proporcao > 0.50) {
                            pontuacao += 50;
                        }

                        if (
                            pontuacao >
                            melhorPontuacao
                        ) {

                            melhorPontuacao =
                                pontuacao;

                            melhor =
                                candidato;
                        }
                    }

                    /*
                     * Se encontrou o jogo,
                     * transforma em tela cheia.
                     */
                    if (melhor) {

                        esconderTudoExceto(melhor);

                        return true;
                    }

                    return false;
                }


                /*
                 * Primeira tentativa.
                 */
                encontrarJogo();


                /*
                 * O jogo pode ser criado depois
                 * por JavaScript.
                 */
                var tentativas = 0;

                var intervalo =
                    setInterval(function() {

                        tentativas++;

                        /*
                         * Depois que o modo jogo foi
                         * aplicado, tenta novamente caso
                         * algum elemento novo seja criado.
                         */
                        encontrarJogo();

                        if (tentativas >= 20) {

                            clearInterval(
                                intervalo
                            );
                        }

                    }, 500);


                /*
                 * Observa mudanças no DOM.
                 */
                try {

                    var observer =
                        new MutationObserver(
                            function() {

                                encontrarJogo();

                            }
                        );

                    observer.observe(
                        document.documentElement,
                        {
                            childList: true,
                            subtree: true
                        }
                    );

                    /*
                     * Para não deixar o observer
                     * rodando indefinidamente.
                     */
                    setTimeout(
                        function() {
                            observer.disconnect();
                        },
                        15000
                    );

                } catch (e) {
                    // Ignora caso MutationObserver não esteja disponível.
                }


                /*
                 * Ajusta novamente quando a tela
                 * muda de tamanho.
                 */
                window.addEventListener(
                    "resize",
                    function() {

                        setTimeout(
                            encontrarJogo,
                            300
                        );

                    }
                );

            })();
        """.trimIndent()

        view.post {

            view.evaluateJavascript(
                javascript,
                null
            )
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

    override fun onResume() {
        super.onResume()

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

        if (
            ::webView.isInitialized &&
            webView.canGoBack()
        ) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}



