package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyzerBridge(
    private val onLog: (String) -> Unit
) {

    @JavascriptInterface
    fun log(message: String) {
        onLog("[JS] $message")
    }

    @JavascriptInterface
    fun sendData(data: String) {
        onLog("[DATA] $data")
    }
}

class MainActivity : ComponentActivity() {

    private val logs = mutableStateListOf<String>()

    private var webViewInstance: WebView? = null

    private var lastSavedFileName: String? = null

    private val gameUrl =
        "https://www.astrocade.com/games/vale-da-heran%C3%A7a-renova%C3%A7%C3%A3o/01M46K2HXGZ4F46MRCKS6WTQ6Y"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appendLog("==============================================")
        appendLog("VALE HERANÇA ANALYZER")
        appendLog("Inicializando analisador...")
        appendLog("URL: $gameUrl")
        appendLog("==============================================")

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {

                    var showConsole by remember {
                        mutableStateOf(false)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        ControlBar(
                            showConsole = showConsole,

                            onToggleConsole = {
                                showConsole = !showConsole
                            },

                            onReload = {
                                appendLog(
                                    "[UI] Recarregando WebView..."
                                )

                                /*
                                 * reload() é uma chamada WebView.
                                 * Garantimos execução na UI thread
                                 * da WebView.
                                 */
                                webViewInstance?.post {

                                    try {
                                        webViewInstance?.reload()
                                    } catch (e: Exception) {

                                        appendLog(
                                            "[WEBVIEW ERROR] reload: ${e.message}"
                                        )
                                    }
                                }
                            },

                            onClearLogs = {

                                logs.clear()

                                appendLog(
                                    "[UI] Log limpo."
                                )
                            },

                            onSaveLog = {
                                saveLogToDownloads()
                            },

                            onCopyLog = {
                                copyLogsToClipboard()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {

                            AndroidView(
                                modifier = Modifier.fillMaxSize(),

                                factory = { context ->

                                    WebView(context).apply {

                                        layoutParams =
                                            ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )

                                        setBackgroundColor(
                                            AndroidColor.BLACK
                                        )

                                        configureWebView(this)

                                        addJavascriptInterface(
                                            AnalyzerBridge {
                                                appendLog(it)
                                            },
                                            "AnalyzerBridge"
                                        )

                                        appendLog(
                                            "[WEBVIEW] Carregando página do jogo..."
                                        )

                                        webViewInstance = this

                                        loadUrl(gameUrl)
                                    }
                                }
                            )
                        }

                        if (showConsole) {

                            ConsoleView(
                                logs = logs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // LOG
    // ============================================================

    private fun appendLog(message: String) {

        runOnUiThread {

            val time = SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.US
            ).format(Date())

            logs.add(
                "[$time] $message"
            )
        }
    }

    // ============================================================
    // WEBVIEW CONFIGURATION
    // ============================================================

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(
        wv: WebView
    ) {

        wv.settings.apply {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            allowFileAccess = true

            allowContentAccess = true

            useWideViewPort = true

            loadWithOverviewMode = false

            mediaPlaybackRequiresUserGesture = false

            javaScriptCanOpenWindowsAutomatically = true

            setSupportMultipleWindows(true)

            cacheMode = WebSettings.LOAD_DEFAULT

            userAgentString =
                "$userAgentString ValeHerancaAnalyzer/1.0"
        }

        appendLog(
            "[WEBVIEW] JavaScript habilitado."
        )

        appendLog(
            "[WEBVIEW] DOM Storage habilitado."
        )

        wv.webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {

                appendLog(
                    "[PAGE START] $url"
                )

                scheduleAnalyzerInjection(view)
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {

                appendLog(
                    "[PAGE FINISHED] $url"
                )

                appendLog(
                    "[PAGE] Injetando analisador JavaScript..."
                )

                scheduleAnalyzerInjection(view)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {

                /*
                 * Este callback pode acontecer em thread
                 * diferente da UI.
                 *
                 * Não fazemos chamadas WebView aqui.
                 */

                if (request != null) {

                    val url =
                        request.url.toString()

                    val method =
                        request.method

                    val headers =
                        request.requestHeaders

                    appendLog(
                        "[REQUEST] $method $url"
                    )

                    if (headers.isNotEmpty()) {

                        appendLog(
                            "[REQUEST HEADERS] " +
                                headers.entries.joinToString()
                        )
                    }
                }

                return super.shouldInterceptRequest(
                    view,
                    request
                )
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {

                if (!url.isNullOrBlank()) {

                    appendLog(
                        "[REQUEST-LEGACY] $url"
                    )
                }

                return super.shouldInterceptRequest(
                    view,
                    url
                )
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {

                appendLog(
                    "[HISTORY] url=$url reload=$isReload"
                )

                super.doUpdateVisitedHistory(
                    view,
                    url,
                    isReload
                )
            }

            override fun onPageCommitVisible(
                view: WebView?,
                url: String?
            ) {

                appendLog(
                    "[PAGE VISIBLE] $url"
                )

                super.onPageCommitVisible(
                    view,
                    url
                )
            }
        }

        wv.webChromeClient =
            object : WebChromeClient() {

                override fun onConsoleMessage(
                    consoleMessage: ConsoleMessage?
                ): Boolean {

                    consoleMessage?.let {

                        appendLog(
                            "[CONSOLE] " +
                                "${it.message()} " +
                                "(${it.sourceId()}:${it.lineNumber()})"
                        )
                    }

                    return true
                }

                override fun onProgressChanged(
                    view: WebView?,
                    newProgress: Int
                ) {

                    if (
                        newProgress == 10 ||
                        newProgress == 25 ||
                        newProgress == 50 ||
                        newProgress == 75 ||
                        newProgress == 90 ||
                        newProgress == 100
                    ) {

                        appendLog(
                            "[PROGRESS] $newProgress%"
                        )
                    }

                    super.onProgressChanged(
                        view,
                        newProgress
                    )
                }
            }
    }

    // ============================================================
    // SAFE JAVASCRIPT INJECTION
    // ============================================================

    private fun scheduleAnalyzerInjection(
        view: WebView?
    ) {

        if (view == null) {
            return
        }

        /*
         * WebView.post() garante que evaluateJavascript()
         * seja executado na UI thread.
         */

        view.post {

            try {

                if (isFinishing) {
                    return@post
                }

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.JELLY_BEAN_MR1
                ) {

                    if (isDestroyed) {
                        return@post
                    }
                }

                val script =
                    createAnalyzerJavascript()

                view.evaluateJavascript(
                    script,
                    null
                )

                appendLog(
                    "[JS] evaluateJavascript enviado para UI thread."
                )

            } catch (e: Exception) {

                appendLog(
                    "[JS ERROR] " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private fun createAnalyzerJavascript(): String {

        return """
        (function() {

            if (window.__VALE_HERANCA_ANALYZER__) {

                console.log(
                    "ValeHerancaAnalyzer já instalado."
                );

                return;
            }

            window.__VALE_HERANCA_ANALYZER__ = true;

            function safeString(value) {

                try {

                    if (typeof value === "string") {
                        return value;
                    }

                    if (value === undefined) {
                        return "undefined";
                    }

                    if (value === null) {
                        return "null";
                    }

                    return JSON.stringify(value);

                } catch (e) {

                    return String(value);
                }
            }

            function send(type, data) {

                try {

                    if (
                        window.AnalyzerBridge &&
                        window.AnalyzerBridge.log
                    ) {

                        window.AnalyzerBridge.log(
                            "[" + type + "] " + data
                        );
                    }

                } catch (e) {

                    console.log(
                        "Bridge error: " + e
                    );
                }
            }

            send(
                "JS",
                "Analyzer instalado em " +
                window.location.href
            );

            send(
                "TITLE",
                document.title
            );

            send(
                "UA",
                navigator.userAgent
            );

            /*
             * FETCH
             */

            try {

                var originalFetch =
                    window.fetch;

                if (originalFetch) {

                    window.fetch =
                        function() {

                            var args =
                                Array.prototype.slice.call(
                                    arguments
                                );

                            var input =
                                args[0];

                            var url =
                                typeof input === "string"
                                    ? input
                                    : (
                                        input &&
                                        input.url
                                        ? input.url
                                        : String(input)
                                    );

                            send(
                                "FETCH",
                                "REQUEST " + url
                            );

                            return originalFetch
                                .apply(
                                    this,
                                    arguments
                                )
                                .then(
                                    function(response) {

                                        send(
                                            "FETCH",
                                            "RESPONSE " +
                                            response.status +
                                            " " +
                                            response.url
                                        );

                                        return response;

                                    }
                                )
                                .catch(
                                    function(error) {

                                        send(
                                            "FETCH",
                                            "ERROR " +
                                            safeString(error)
                                        );

                                        throw error;
                                    }
                                );
                        };

                    send(
                        "HOOK",
                        "fetch interceptado"
                    );
                }

            } catch (e) {

                send(
                    "HOOK ERROR",
                    "fetch: " + e
                );
            }

            /*
             * XMLHttpRequest
             */

            try {

                var originalOpen =
                    XMLHttpRequest.prototype.open;

                var originalSend =
                    XMLHttpRequest.prototype.send;

                XMLHttpRequest.prototype.open =
                    function(
                        method,
                        url
                    ) {

                        this.__vh_method =
                            method;

                        this.__vh_url =
                            url;

                        send(
                            "XHR",
                            "OPEN " +
                            method +
                            " " +
                            url
                        );

                        return originalOpen.apply(
                            this,
                            arguments
                        );
                    };

                XMLHttpRequest.prototype.send =
                    function(body) {

                        send(
                            "XHR",
                            "SEND " +
                            (this.__vh_method || "") +
                            " " +
                            (this.__vh_url || "")
                        );

                        if (body) {

                            send(
                                "XHR BODY",
                                safeString(body)
                            );
                        }

                        return originalSend.apply(
                            this,
                            arguments
                        );
                    };

                send(
                    "HOOK",
                    "XMLHttpRequest interceptado"
                );

            } catch (e) {

                send(
                    "HOOK ERROR",
                    "XHR: " + e
                );
            }

            /*
             * WEBSOCKET
             */

            try {

                var OriginalWebSocket =
                    window.WebSocket;

                if (OriginalWebSocket) {

                    window.WebSocket =
                        function(
                            url,
                            protocols
                        ) {

                            send(
                                "WEBSOCKET",
                                "CONNECT " + url
                            );

                            var socket;

                            if (
                                protocols !== undefined
                            ) {

                                socket =
                                    new OriginalWebSocket(
                                        url,
                                        protocols
                                    );

                            } else {

                                socket =
                                    new OriginalWebSocket(
                                        url
                                    );
                            }

                            socket.addEventListener(
                                "open",
                                function() {

                                    send(
                                        "WEBSOCKET",
                                        "OPEN " + url
                                    );
                                }
                            );

                            socket.addEventListener(
                                "message",
                                function(event) {

                                    send(
                                        "WEBSOCKET MESSAGE",
                                        safeString(
                                            event.data
                                        )
                                    );
                                }
                            );

                            socket.addEventListener(
                                "close",
                                function(event) {

                                    send(
                                        "WEBSOCKET",
                                        "CLOSE " +
                                        url +
                                        " code=" +
                                        event.code
                                    );
                                }
                            );

                            socket.addEventListener(
                                "error",
                                function() {

                                    send(
                                        "WEBSOCKET",
                                        "ERROR " +
                                        url
                                    );
                                }
                            );

                            return socket;
                        };

                    window.WebSocket.prototype =
                        OriginalWebSocket.prototype;

                    send(
                        "HOOK",
                        "WebSocket interceptado"
                    );
                }

            } catch (e) {

                send(
                    "HOOK ERROR",
                    "WebSocket: " + e
                );
            }

            /*
             * DOM
             */

            try {

                var links =
                    document.querySelectorAll("a");

                send(
                    "DOM",
                    "Links encontrados: " +
                    links.length
                );

                for (
                    var i = 0;
                    i < Math.min(
                        links.length,
                        100
                    );
                    i++
                ) {

                    var href =
                        links[i].href;

                    if (href) {

                        send(
                            "LINK",
                            href
                        );
                    }
                }

            } catch (e) {

                send(
                    "DOM ERROR",
                    String(e)
                );
            }

            /*
             * RESOURCES
             */

            try {

                var resources =
                    performance.getEntriesByType(
                        "resource"
                    );

                send(
                    "RESOURCES",
                    "Recursos carregados: " +
                    resources.length
                );

                for (
                    var r = 0;
                    r < resources.length;
                    r++
                ) {

                    if (
                        resources[r] &&
                        resources[r].name
                    ) {

                        send(
                            "RESOURCE",
                            resources[r].name
                        );
                    }
                }

            } catch (e) {

                send(
                    "RESOURCE ERROR",
                    String(e)
                );
            }

            /*
             * PAGE INFO
             */

            try {

                send(
                    "PAGE INFO",
                    "URL=" +
                    window.location.href +
                    " | TITLE=" +
                    document.title +
                    " | ORIGIN=" +
                    window.location.origin
                );

            } catch (e) {

                send(
                    "PAGE INFO ERROR",
                    String(e)
                );
            }

            console.log(
                "ValeHerancaAnalyzer instalado."
            );

        })();
        """.trimIndent()
    }

    // ============================================================
    // RELATÓRIO
    // ============================================================

    private fun buildCompleteLog(
        userAgent: String,
        logSnapshot: List<String>
    ): String {

        val builder =
            StringBuilder()

        builder.append(
            "VALE DA HERANÇA - RELATÓRIO DE ANÁLISE\n"
        )

        builder.append(
            "==============================================\n\n"
        )

        builder.append(
            "Data da análise: "
        )

        builder.append(
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).format(Date())
        )

        builder.append(
            "\n\n"
        )

        builder.append(
            "URL inicial:\n"
        )

        builder.append(
            gameUrl
        )

        builder.append(
            "\n\n"
        )

        builder.append(
            "Android:\n"
        )

        builder.append(
            Build.VERSION.RELEASE
        )

        builder.append(
            " (API "
        )

        builder.append(
            Build.VERSION.SDK_INT
        )

        builder.append(
            ")\n\n"
        )

        builder.append(
            "Dispositivo:\n"
        )

        builder.append(
            Build.MANUFACTURER
        )

        builder.append(
            " "
        )

        builder.append(
            Build.MODEL
        )

        builder.append(
            "\n\n"
        )

        builder.append(
            "USER AGENT:\n"
        )

        builder.append(
            userAgent
        )

        builder.append(
            "\n\n"
        )

        builder.append(
            "==============================================\n"
        )

        builder.append(
            "LOG DE EVENTOS\n"
        )

        builder.append(
            "==============================================\n\n"
        )

        /*
         * CORREÇÃO:
         *
         * Não usar:
         *
         * for (log: String in logSnapshot)
         *
         * O tipo já é inferido pela List<String>.
         */

        for (log in logSnapshot) {

            builder.append(log)

            builder.append("\n")
        }

        builder.append(
            "\n==============================================\n"
        )

        builder.append(
            "FIM DO RELATÓRIO\n"
        )

        builder.append(
            "==============================================\n"
        )

        return builder.toString()
    }

    // ============================================================
    // SALVAR TXT
    // ============================================================

    private fun saveLogToDownloads() {

        /*
         * Esta função começa na UI thread.
         *
         * O WebView é acessado somente antes de
         * iniciar a thread de gravação.
         */

        runOnUiThread {

            try {

                val userAgent =
                    webViewInstance
                        ?.settings
                        ?.userAgentString
                        ?: "N/D"

                val logSnapshot: List<String> =
                    logs.toList()

                val reportContent =
                    buildCompleteLog(
                        userAgent = userAgent,
                        logSnapshot = logSnapshot
                    )

                /*
                 * Daqui para frente a thread recebe apenas
                 * String imutável.
                 *
                 * Nenhum WebView é acessado.
                 */

                Thread {

                    saveReportFile(
                        reportContent
                    )

                }.start()

            } catch (e: Exception) {

                appendLog(
                    "[SAVE ERROR] " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )

                Toast.makeText(
                    this,
                    "Erro ao preparar relatório",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun saveReportFile(
        reportContent: String
    ) {

        try {

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())

            val fileName =
                "vale_heranca_analise_$timestamp.txt"

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val resolver =
                    contentResolver

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Downloads.DISPLAY_NAME,
                            fileName
                        )

                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            "text/plain"
                        )

                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )

                        put(
                            MediaStore.Downloads.IS_PENDING,
                            1
                        )
                    }

                val uri =
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )

                if (uri == null) {

                    throw Exception(
                        "Não foi possível criar o arquivo."
                    )
                }

                try {

                    resolver
                        .openOutputStream(uri)
                        .use { output ->

                            if (output == null) {

                                throw Exception(
                                    "Não foi possível abrir o arquivo."
                                )
                            }

                            output.write(
                                reportContent.toByteArray(
                                    Charsets.UTF_8
                                )
                            )

                            output.flush()
                        }

                    val completed =
                        ContentValues().apply {

                            put(
                                MediaStore.Downloads.IS_PENDING,
                                0
                            )
                        }

                    resolver.update(
                        uri,
                        completed,
                        null,
                        null
                    )

                } catch (e: Exception) {

                    resolver.delete(
                        uri,
                        null,
                        null
                    )

                    throw e
                }

            } else {

                @Suppress("DEPRECATION")
                val downloads =
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                if (!downloads.exists()) {

                    downloads.mkdirs()
                }

                val file =
                    File(
                        downloads,
                        fileName
                    )

                file.writeText(
                    reportContent,
                    Charsets.UTF_8
                )
            }

            runOnUiThread {

                lastSavedFileName =
                    fileName

                appendLog(
                    "[SAVE] Arquivo salvo: Downloads/$fileName"
                )

                Toast.makeText(
                    this,
                    "Relatório salvo em Downloads",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            runOnUiThread {

                appendLog(
                    "[SAVE ERROR] " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )

                Toast.makeText(
                    this,
                    "Erro ao salvar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // COPIAR
    // ============================================================

    private fun copyLogsToClipboard() {

        runOnUiThread {

            try {

                val userAgent =
                    webViewInstance
                        ?.settings
                        ?.userAgentString
                        ?: "N/D"

                val logSnapshot: List<String> =
                    logs.toList()

                val content =
                    buildCompleteLog(
                        userAgent = userAgent,
                        logSnapshot = logSnapshot
                    )

                val clipboard =
                    getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "Vale Herança Analyzer",
                        content
                    )
                )

                appendLog(
                    "[UI] Relatório copiado."
                )

                Toast.makeText(
                    this,
                    "Relatório copiado",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                appendLog(
                    "[COPY ERROR] ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onResume() {

        super.onResume()

        /*
         * onResume ocorre na UI thread.
         */
        webViewInstance?.onResume()
    }

    override fun onPause() {

        /*
         * onPause ocorre na UI thread.
         */
        webViewInstance?.onPause()

        super.onPause()
    }

    override fun onDestroy() {

        /*
         * onDestroy também ocorre na UI thread.
         */

        webViewInstance?.apply {

            try {
                stopLoading()
            } catch (_) {
            }

            try {
                removeAllViews()
            } catch (_) {
            }

            try {
                destroy()
            } catch (_) {
            }
        }

        webViewInstance = null

        super.onDestroy()
    }
}

// ================================================================
// CONTROL BAR
// ================================================================

@Composable
fun ControlBar(
    showConsole: Boolean,
    onToggleConsole: () -> Unit,
    onReload: () -> Unit,
    onClearLogs: () -> Unit,
    onSaveLog: () -> Unit,
    onCopyLog: () -> Unit
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF1E1E1E)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(6.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Button(

            onClick = onReload,

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF333333)
                )
        ) {

            Text(
                text = "Recarregar",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Button(

            onClick = onToggleConsole,

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (showConsole)
                            Color(0xFF007ACC)
                        else
                            Color(0xFF333333)
                )
        ) {

            Text(
                text =
                    if (showConsole)
                        "Ocultar Log"
                    else
                        "Ver Log",

                fontSize = 11.sp,

                color = Color.White
            )
        }

        Button(

            onClick = onSaveLog,

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF176B3A)
                )
        ) {

            Text(
                text = "SALVAR TXT",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Button(

            onClick = onCopyLog,

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF444444)
                )
        ) {

            Text(
                text = "COPIAR",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        if (showConsole) {

            Button(

                onClick = onClearLogs,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF552222)
                    )
            ) {

                Text(
                    text = "LIMPAR",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ================================================================
// CONSOLE
// ================================================================

@Composable
fun ConsoleView(
    logs: List<String>,
    modifier: Modifier = Modifier
) {

    val listState =
        rememberLazyListState()

    LaunchedEffect(
        logs.size
    ) {

        if (logs.isNotEmpty()) {

            listState.animateScrollToItem(
                logs.size - 1
            )
        }
    }

    LazyColumn(

        state = listState,

        modifier = modifier
            .background(
                Color(0xFF0D0D0D)
            )
            .padding(6.dp)
    ) {

        items(
            items = logs
        ) { log ->

            Text(

                text = log,

                color =
                    when {

                        log.contains("[CONSOLE]") ->
                            Color(0xFF4EC9B0)

                        log.contains("[REQUEST]") ->
                            Color(0xFF569CD6)

                        log.contains("[FETCH]") ->
                            Color(0xFFDCDCAA)

                        log.contains("[XHR]") ->
                            Color(0xFFCE9178)

                        log.contains("[WEBSOCKET]") ->
                            Color(0xFFC586C0)

                        log.contains("[RESOURCE]") ->
                            Color(0xFF9CDCFE)

                        log.contains("[PAGE]") ->
                            Color(0xFFCE9178)

                        log.contains("[SAVE]") ->
                            Color(0xFF6A9955)

                        else ->
                            Color(0xFFCCCCCC)
                    },

                fontSize = 10.sp,

                fontFamily =
                    FontFamily.Monospace,

                lineHeight = 13.sp
            )
        }
    }
}


