package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : ComponentActivity() {

    companion object {

        private const val GAME_URL =
            "https://www.astrocade.com/games/vale-da-heran%C3%A7a-renova%C3%A7%C3%A3o/01M46K2HXGZ4F46MRCKS6WTQ6Y?sharedByCreator=rgr.iereme&surface=web&sharePlatform=copylink&shareId=510ef8fa-6cbc-48f6-8102-88fcb002dcb0&shareOrigin=editor"

        private const val REPORT_FILE_NAME =
            "vale_heranca_analise.txt"
    }

    private lateinit var webView: WebView

    private val handler =
        Handler(Looper.getMainLooper())

    private val requestCounter =
        AtomicInteger(0)

    private var logText by mutableStateOf("")

    private var pageTitle by mutableStateOf(
        "Aguardando..."
    )

    private var currentUrl by mutableStateOf("")

    private var reportUri: Uri? = null

    private var saveScheduled = false

    private val saveRunnable = Runnable {
        saveScheduled = false
        saveReport()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        createReportFile()

        setContent {

            AnalyzerScreen(
                log = logText,
                title = pageTitle,
                url = currentUrl,

                onReload = {

                    if (::webView.isInitialized) {

                        addLog(
                            "===== RECARREGANDO ====="
                        )

                        webView.reload()
                    }
                },

                onBack = {

                    if (
                        ::webView.isInitialized &&
                        webView.canGoBack()
                    ) {

                        addLog(
                            "[NAV] VOLTAR"
                        )

                        webView.goBack()
                    }
                },

                onForward = {

                    if (
                        ::webView.isInitialized &&
                        webView.canGoForward()
                    ) {

                        addLog(
                            "[NAV] AVANÇAR"
                        )

                        webView.goForward()
                    }
                },

                onCopy = {
                    copyLog()
                },

                onClear = {
                    clearLog()
                },

                onSave = {

                    saveReport()

                    Toast.makeText(
                        this,
                        "Relatório salvo em Downloads",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onWebViewCreated = { view ->

                    webView = view

                    configureWebView(view)

                    addLog(
                        "========================================"
                    )

                    addLog(
                        "VALE HERANÇA ANALYZER"
                    )

                    addLog(
                        "========================================"
                    )

                    addLog(
                        "Aplicativo iniciado"
                    )

                    addLog(
                        "Android: " +
                                android.os.Build.VERSION.RELEASE
                    )

                    addLog(
                        "SDK: " +
                                android.os.Build.VERSION.SDK_INT
                    )

                    addLog(
                        "WebView: " +
                                getWebViewVersion()
                    )

                    addLog(
                        "Relatório:"
                    )

                    addLog(
                        "Downloads/$REPORT_FILE_NAME"
                    )

                    addLog("")

                    addLog(
                        "URL inicial:"
                    )

                    addLog(GAME_URL)

                    addLog("")

                    addLog(
                        "Carregando jogo..."
                    )

                    addLog("")

                    view.loadUrl(GAME_URL)
                }
            )
        }
    }

    // ============================================================
    // CONFIGURAÇÃO WEBVIEW
    // ============================================================

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(
        view: WebView
    ) {

        val settings = view.settings

        settings.javaScriptEnabled = true

        settings.domStorageEnabled = true

        settings.databaseEnabled = true

        settings.allowFileAccess = true

        settings.allowContentAccess = true

        settings.loadsImagesAutomatically = true

        settings.blockNetworkImage = false

        settings.javaScriptCanOpenWindowsAutomatically =
            true

        settings.setSupportMultipleWindows(false)

        settings.mediaPlaybackRequiresUserGesture =
            false

        settings.cacheMode =
            WebSettings.LOAD_DEFAULT

        settings.userAgentString =
            settings.userAgentString +
                    " ValeHerancaAnalyzer/1.0"

        CookieManager
            .getInstance()
            .setAcceptCookie(true)

        CookieManager
            .getInstance()
            .setAcceptThirdPartyCookies(
                view,
                true
            )

        view.addJavascriptInterface(
            JsBridge(),
            "VALE_ANALYZER"
        )

        view.webViewClient =
            createWebViewClient()

        view.webChromeClient =
            createWebChromeClient()
    }

    // ============================================================
    // WEBVIEW CLIENT
    // ============================================================

    private fun createWebViewClient():
            WebViewClient {

        return object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                addLog(
                    "[NAVIGATION] " +
                            request.method +
                            " " +
                            request.url
                )

                return false
            }

            override fun onPageStarted(
                view: WebView,
                url: String,
                favicon: android.graphics.Bitmap?
            ) {

                super.onPageStarted(
                    view,
                    url,
                    favicon
                )

                currentUrl = url

                addLog("")

                addLog(
                    "========================================"
                )

                addLog(
                    "[PAGE START]"
                )

                addLog(url)

                addLog(
                    "========================================"
                )
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {

                super.onPageFinished(
                    view,
                    url
                )

                currentUrl = url

                pageTitle =
                    view.title
                        ?: "Sem título"

                addLog("")

                addLog(
                    "[PAGE FINISHED]"
                )

                addLog(
                    "Título: ${view.title}"
                )

                addLog(
                    "URL: $url"
                )

                val cookies =
                    CookieManager
                        .getInstance()
                        .getCookie(url)

                if (!cookies.isNullOrBlank()) {

                    addLog(
                        "[COOKIE] " +
                                "${cookies.length} caracteres"
                    )

                } else {

                    addLog(
                        "[COOKIE] Nenhum cookie"
                    )
                }

                addLog("")

                addLog(
                    "[JAVASCRIPT] " +
                            "Instalando analisador..."
                )

                injectJavaScriptAnalyzer(view)
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {

                logResourceRequest(
                    request.method,
                    request.url.toString(),
                    request.isForMainFrame
                )

                return null
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {

                super.onReceivedError(
                    view,
                    request,
                    error
                )

                addLog("")

                addLog(
                    "[WEB ERROR]"
                )

                addLog(
                    "URL: ${request.url}"
                )

                addLog(
                    "Código: ${error.errorCode}"
                )

                addLog(
                    "Descrição: ${error.description}"
                )
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {

                super.onReceivedHttpError(
                    view,
                    request,
                    errorResponse
                )

                addLog("")

                addLog(
                    "[HTTP ERROR]"
                )

                addLog(
                    "URL: ${request.url}"
                )

                addLog(
                    "Status: " +
                            errorResponse.statusCode
                )

                addLog(
                    "Mensagem: " +
                            errorResponse.reasonPhrase
                )
            }
        }
    }

    // ============================================================
    // WEB CHROME
    // ============================================================

    private fun createWebChromeClient():
            WebChromeClient {

        return object : WebChromeClient() {

            override fun onConsoleMessage(
                message: ConsoleMessage
            ): Boolean {

                addLog(
                    "[JS CONSOLE] " +
                            message.messageLevel() +
                            " | " +
                            message.message() +
                            " | linha " +
                            message.lineNumber()
                )

                return true
            }

            override fun onProgressChanged(
                view: WebView,
                progress: Int
            ) {

                super.onProgressChanged(
                    view,
                    progress
                )

                if (progress == 100) {

                    addLog(
                        "[PAGE] carregamento 100%"
                    )
                }
            }
        }
    }

    // ============================================================
    // REQUISIÇÕES
    // ============================================================

    private fun logResourceRequest(
        method: String,
        url: String,
        mainFrame: Boolean
    ) {

        val id =
            requestCounter.incrementAndGet()

        val type =
            detectResourceType(url)

        val frame =
            if (mainFrame) {
                "[MAIN] "
            } else {
                ""
            }

        addLog(
            "[REQ #$id] " +
                    "$method " +
                    "$frame" +
                    "[$type] " +
                    url
        )
    }

    private fun detectResourceType(
        url: String
    ): String {

        val lower =
            url.lowercase(Locale.US)

        return when {

            lower.contains(".js") ||
                    lower.contains("javascript") ->
                "JS"

            lower.contains(".css") ->
                "CSS"

            lower.contains(".json") ||
                    lower.contains("json") ->
                "JSON"

            lower.contains(".png") ||
                    lower.contains(".jpg") ||
                    lower.contains(".jpeg") ||
                    lower.contains(".webp") ||
                    lower.contains(".gif") ||
                    lower.contains(".svg") ||
                    lower.contains(".avif") ->
                "IMAGE"

            lower.contains(".mp3") ||
                    lower.contains(".wav") ||
                    lower.contains(".ogg") ||
                    lower.contains(".m4a") ||
                    lower.contains(".aac") ->
                "AUDIO"

            lower.contains(".mp4") ||
                    lower.contains(".webm") ||
                    lower.contains(".m3u8") ->
                "VIDEO"

            lower.contains(".woff") ||
                    lower.contains(".woff2") ||
                    lower.contains(".ttf") ||
                    lower.contains(".otf") ->
                "FONT"

            lower.contains(".wasm") ->
                "WASM"

            lower.contains("/api/") ||
                    lower.contains("/api") ->
                "API"

            lower.startsWith("data:") ->
                "DATA"

            else ->
                "OTHER"
        }
    }

    // ============================================================
    // JAVASCRIPT
    // ============================================================

    private fun injectJavaScriptAnalyzer(
        view: WebView
    ) {

        val script = """
            (function() {

                if (window.__VALE_ANALYZER__) {
                    return;
                }

                window.__VALE_ANALYZER__ = true;

                function send(type, data) {

                    try {

                        window.VALE_ANALYZER.log(
                            type + "|" + String(data)
                        );

                    } catch(e) {}
                }

                send(
                    "PAGE",
                    document.title +
                    " | " +
                    location.href
                );

                /*
                 * FETCH
                 */

                try {

                    const originalFetch =
                        window.fetch;

                    window.fetch =
                        function() {

                            try {

                                let input =
                                    arguments[0];

                                let url =
                                    typeof input === "string"
                                    ? input
                                    : (
                                        input &&
                                        input.url
                                        ? input.url
                                        : String(input)
                                    );

                                let method = "GET";

                                if (
                                    arguments[1] &&
                                    arguments[1].method
                                ) {

                                    method =
                                        arguments[1].method;
                                }

                                send(
                                    "FETCH",
                                    method +
                                    " " +
                                    url
                                );

                            } catch(e) {}

                            return originalFetch.apply(
                                this,
                                arguments
                            );
                        };

                } catch(e) {

                    send(
                        "ERROR",
                        "fetch hook " + e
                    );
                }

                /*
                 * XHR
                 */

                try {

                    const originalOpen =
                        XMLHttpRequest
                            .prototype
                            .open;

                    XMLHttpRequest
                        .prototype
                        .open =
                        function(
                            method,
                            url
                        ) {

                            try {

                                send(
                                    "XHR",
                                    String(method) +
                                    " " +
                                    String(url)
                                );

                            } catch(e) {}

                            return originalOpen.apply(
                                this,
                                arguments
                            );
                        };

                } catch(e) {

                    send(
                        "ERROR",
                        "XHR hook " + e
                    );
                }

                /*
                 * WEBSOCKET
                 */

                try {

                    const OriginalWebSocket =
                        window.WebSocket;

                    window.WebSocket =
                        function(
                            url,
                            protocols
                        ) {

                            try {

                                send(
                                    "WEBSOCKET",
                                    String(url)
                                );

                            } catch(e) {}

                            if (
                                protocols !== undefined
                            ) {

                                return new OriginalWebSocket(
                                    url,
                                    protocols
                                );

                            }

                            return new OriginalWebSocket(
                                url
                            );
                        };

                    window.WebSocket.prototype =
                        OriginalWebSocket.prototype;

                } catch(e) {

                    send(
                        "ERROR",
                        "WebSocket hook " + e
                    );
                }

                /*
                 * CONSOLE.LOG
                 */

                try {

                    const originalLog =
                        console.log;

                    console.log =
                        function() {

                            try {

                                send(
                                    "CONSOLE",
                                    Array
                                        .prototype
                                        .slice
                                        .call(arguments)
                                        .join(" ")
                                );

                            } catch(e) {}

                            originalLog.apply(
                                console,
                                arguments
                            );
                        };

                } catch(e) {}

                /*
                 * INFORMAÇÕES
                 */

                try {

                    send(
                        "INFO",
                        "viewport=" +
                        window.innerWidth +
                        "x" +
                        window.innerHeight
                    );

                    send(
                        "INFO",
                        "devicePixelRatio=" +
                        window.devicePixelRatio
                    );

                    send(
                        "INFO",
                        "language=" +
                        navigator.language
                    );

                    send(
                        "INFO",
                        "userAgent=" +
                        navigator.userAgent
                    );

                } catch(e) {}

            })();
        """.trimIndent()

        view.evaluateJavascript(
            script,
            null
        )
    }

    // ============================================================
    // BRIDGE
    // ============================================================

    inner class JsBridge {

        @JavascriptInterface
        fun log(
            message: String?
        ) {

            if (message.isNullOrBlank()) {
                return
            }

            runOnUiThread {

                val separator =
                    message.indexOf("|")

                if (separator <= 0) {

                    addLog(
                        "[JS] $message"
                    )

                    return@runOnUiThread
                }

                val type =
                    message.substring(
                        0,
                        separator
                    )

                val data =
                    message.substring(
                        separator + 1
                    )

                addLog(
                    "[JS $type] $data"
                )
            }
        }
    }

    // ============================================================
    // LOG
    // ============================================================

    private fun addLog(
        message: String
    ) {

        val timestamp =
            SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.getDefault()
            ).format(Date())

        val line =
            "[$timestamp] $message"

        logText =
            if (logText.isEmpty()) {
                line
            } else {
                logText + "\n" + line
            }

        if (logText.length > 5_000_000) {

            logText =
                logText.takeLast(4_000_000)
        }

        scheduleSave()
    }

    private fun clearLog() {

        logText = ""

        requestCounter.set(0)

        addLog(
            "===== LOG LIMPO ====="
        )
    }

    // ============================================================
    // ARQUIVO TXT
    // ============================================================

    private fun createReportFile() {

        try {

            val resolver =
                contentResolver

            val collection =
                MediaStore.Downloads
                    .EXTERNAL_CONTENT_URI

            val projection =
                arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME
                )

            resolver.query(
                collection,
                projection,
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(REPORT_FILE_NAME),
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val id =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID
                            )
                        )

                    reportUri =
                        Uri.withAppendedPath(
                            collection,
                            id.toString()
                        )

                    return
                }
            }

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        REPORT_FILE_NAME
                    )

                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "text/plain"
                    )

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }

            reportUri =
                resolver.insert(
                    collection,
                    values
                )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun scheduleSave() {

        if (saveScheduled) {
            return
        }

        saveScheduled = true

        handler.postDelayed(
            saveRunnable,
            1500
        )
    }

    private fun saveReport() {

        val uri =
            reportUri
                ?: run {

                    createReportFile()

                    reportUri
                }
                ?: return

        try {

            contentResolver
                .openOutputStream(
                    uri,
                    "wt"
                )
                ?.use { output ->

                    val header = """
                        ========================================
                        VALE HERANÇA ANALYZER
                        RELATÓRIO DE ANÁLISE
                        ========================================

                        Data:
                        ${Date()}

                        Página:
                        $currentUrl

                        Título:
                        $pageTitle

                        Requisições observadas:
                        ${requestCounter.get()}

                        ========================================
                        LOG
                        ========================================

                    """.trimIndent()

                    output.write(
                        header.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                    output.write(
                        logText.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                    output.flush()
                }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // ============================================================
    // COPIAR
    // ============================================================

    private fun copyLog() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vale Herança Log",
                logText
            )
        )

        Toast.makeText(
            this,
            "Log copiado",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // WEBVIEW VERSION
    // ============================================================

    private fun getWebViewVersion(): String {

        return try {

            val info =
                packageManager.getPackageInfo(
                    "com.google.android.webview",
                    0
                )

            info.versionName
                ?: "desconhecida"

        } catch (e: Exception) {

            try {

                val info =
                    packageManager.getPackageInfo(
                        "com.android.webview",
                        0
                    )

                info.versionName
                    ?: "desconhecida"

            } catch (e2: Exception) {

                "não identificada"
            }
        }
    }

    override fun onDestroy() {

        saveReport()

        handler.removeCallbacks(
            saveRunnable
        )

        if (::webView.isInitialized) {

            webView.stopLoading()

            webView.destroy()
        }

        super.onDestroy()
    }
}

// ================================================================
// TELA
// ================================================================

@androidx.compose.runtime.Composable
private fun AnalyzerScreen(
    log: String,
    title: String,
    url: String,
    onReload: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {

    val scrollState =
        rememberScrollState()

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black
                )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {

                Text(
                    text = "VALE HERANÇA",
                    color = Color.White,
                    fontSize = 19.sp
                )

                Text(
                    text = "ANALYZER",
                    color = Color(
                        android.graphics.Color.rgb(
                            229,
                            9,
                            20
                        )
                    ),
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier
                        .height(5.dp)
                )

                Text(
                    text = title,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                Text(
                    text = url,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 2
                )
            }

            androidx.compose.material3.HorizontalDivider(
                color = Color.DarkGray
            )

            /*
             * JOGO
             */

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color.White
                    )
            ) {

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize(),

                    factory = { context ->

                        WebView(context).also {
                            onWebViewCreated(it)
                        }
                    }
                )
            }

            /*
             * LOG
             */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Color(
                            android.graphics.Color.rgb(
                                8,
                                8,
                                8
                            )
                        )
                    )
                    .verticalScroll(
                        scrollState
                    )
                    .padding(8.dp)
            ) {

                Text(
                    text = log,
                    color = Color(
                        android.graphics.Color.rgb(
                            53,
                            199,
                            89
                        )
                    ),
                    fontSize = 9.sp
                )
            }

            /*
             * CONTROLES
             */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black
                    )
                    .padding(6.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            5.dp
                        )
                ) {

                    Button(
                        onClick = onBack,
                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            android.graphics.Color.rgb(
                                                35,
                                                35,
                                                35
                                            )
                                        )
                                )
                    ) {

                        Text("←")
                    }

                    Button(
                        onClick = onForward,
                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            android.graphics.Color.rgb(
                                                35,
                                                35,
                                                35
                                            )
                                        )
                                )
                    ) {

                        Text("→")
                    }

                    Button(
                        onClick = onReload,
                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            android.graphics.Color.rgb(
                                                35,
                                                35,
                                                35
                                            )
                                        )
                                )
                    ) {

                        Text("↻")
                    }
                }

                Spacer(
                    modifier = Modifier
                        .height(5.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            5.dp
                        )
                ) {

                    OutlinedButton(
                        onClick = onCopy,
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text("COPIAR")
                    }

                    OutlinedButton(
                        onClick = onClear,
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text("LIMPAR")
                    }

                    Button(
                        onClick = onSave,
                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            android.graphics.Color.rgb(
                                                229,
                                                9,
                                                20
                                            )
                                        )
                                )
                    ) {

                        Text("SALVAR TXT")
                    }
                }
            }
        }
    }
}



