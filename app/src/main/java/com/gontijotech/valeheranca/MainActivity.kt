package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

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

    companion object {

        private const val GAME_URL =
            "https://www.astrocade.com/games/vale-da-heran%C3%A7a-renova%C3%A7%C3%A3o/01M46K2HXGZ4F46MRCKS6WTQ6Y"

        private const val REPORT_FILE_NAME =
            "vale_heranca_analise.txt"
    }

    private val logs =
        mutableStateListOf<String>()

    private var webViewInstance: WebView? = null

    private val requestCounter =
        AtomicInteger(0)

    private var saveStatus by mutableStateOf("")

    private var lastUrl =
        GAME_URL

    private var pageStartTime =
        System.currentTimeMillis()

    fun appendLog(message: String) {

        runOnUiThread {

            val timestamp =
                SimpleDateFormat(
                    "HH:mm:ss.SSS",
                    Locale.US
                ).format(Date())

            logs.add(
                "[$timestamp] $message"
            )
        }
    }

    fun getLogs(): List<String> =
        logs.toList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        appendLog(
            "=================================================="
        )

        appendLog(
            "VALE HERANÇA ANALYZER INICIADO"
        )

        appendLog(
            "Android SDK: ${Build.VERSION.SDK_INT}"
        )

        appendLog(
            "Modelo: ${Build.MANUFACTURER} ${Build.MODEL}"
        )

        appendLog(
            "=================================================="
        )

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {

                    var showConsole by remember {
                        mutableStateOf(true)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Text(
                            text = "VALE HERANÇA ANALYZER",
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.Black
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 8.dp
                                )
                        )

                        ControlBar(
                            showConsole = showConsole,
                            saveStatus = saveStatus,
                            onToggleConsole = {
                                showConsole =
                                    !showConsole
                            },
                            onReload = {
                                webViewInstance?.reload()
                            },
                            onClearLogs = {
                                logs.clear()

                                appendLog(
                                    "[UI] Log limpo."
                                )
                            },
                            onCopyLogs = {
                                copyLogs()
                            },
                            onSaveLogs = {
                                saveReportToDownloads()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {

                            AndroidView(
                                modifier =
                                    Modifier.fillMaxSize(),

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

                                        configureWebView(
                                            this
                                        )

                                        addJavascriptInterface(
                                            AnalyzerBridge {
                                                appendLog(it)
                                            },
                                            "AnalyzerBridge"
                                        )

                                        webViewInstance =
                                            this

                                        loadUrl(
                                            GAME_URL
                                        )
                                    }
                                }
                            )
                        }

                        if (showConsole) {

                            ConsoleView(
                                logs = logs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(
        wv: WebView
    ) {

        wv.settings.apply {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            useWideViewPort = true

            loadWithOverviewMode = true

            mediaPlaybackRequiresUserGesture =
                false

            javaScriptCanOpenWindowsAutomatically =
                true

            setSupportMultipleWindows(true)

            allowFileAccess = true

            allowContentAccess = true

            loadsImagesAutomatically = true

            mixedContentMode =
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            cacheMode =
                WebSettings.LOAD_DEFAULT

            userAgentString =
                userAgentString +
                        " ValeHerancaAnalyzer/1.0"
        }

        wv.webViewClient =
            object : WebViewClient() {

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: Bitmap?
                ) {

                    super.onPageStarted(
                        view,
                        url,
                        favicon
                    )

                    pageStartTime =
                        System.currentTimeMillis()

                    lastUrl =
                        url ?: ""

                    appendLog(
                        ""
                    )

                    appendLog(
                        "========== PAGE START =========="
                    )

                    appendLog(
                        "[PAGE] Iniciando: $url"
                    )

                    appendLog(
                        "[PAGE] Cookies: ${
                            getCookieInfo(url)
                        }"
                    )
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    lastUrl =
                        url ?: ""

                    val elapsed =
                        System.currentTimeMillis() -
                                pageStartTime

                    appendLog(
                        "[PAGE] Concluído: $url"
                    )

                    appendLog(
                        "[PAGE] Tempo: ${elapsed} ms"
                    )

                    appendLog(
                        "[PAGE] Título: ${
                            view?.title ?: ""
                        }"
                    )

                    injectJavaScriptHooks(
                        view
                    )
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {

                    val url =
                        request?.url?.toString()
                            ?: ""

                    appendLog(
                        "[NAVIGATION] $url"
                    )

                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {

                    request?.let {

                        val id =
                            requestCounter
                                .incrementAndGet()

                        val url =
                            it.url.toString()

                        val method =
                            it.method

                        val type =
                            classifyRequest(
                                url,
                                it.requestHeaders
                            )

                        appendLog(
                            "[REQ #$id] $method [$type] $url"
                        )

                        if (
                            it.requestHeaders.isNotEmpty()
                        ) {

                            appendLog(
                                "[REQ #$id HEADERS] ${
                                    it.requestHeaders
                                        .entries
                                        .joinToString(
                                            "; "
                                        )
                                }"
                            )
                        }
                    }

                    return super
                        .shouldInterceptRequest(
                            view,
                            request
                        )
                }

                @Suppress("DEPRECATION")
                override fun shouldInterceptRequest(
                    view: WebView?,
                    url: String?
                ): WebResourceResponse? {

                    if (
                        !url.isNullOrBlank()
                    ) {

                        val id =
                            requestCounter
                                .incrementAndGet()

                        appendLog(
                            "[REQ #$id] GET [LEGACY] $url"
                        )
                    }

                    return super
                        .shouldInterceptRequest(
                            view,
                            url
                        )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {

                    super.onReceivedError(
                        view,
                        request,
                        error
                    )

                    appendLog(
                        "[WEB ERROR] " +
                                "url=${request?.url} " +
                                "code=${error?.errorCode} " +
                                "description=${error?.description}"
                    )
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {

                    super.onReceivedHttpError(
                        view,
                        request,
                        errorResponse
                    )

                    appendLog(
                        "[HTTP ERROR] " +
                                "url=${request?.url} " +
                                "status=${errorResponse?.statusCode} " +
                                "reason=${errorResponse?.reasonPhrase}"
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
                        newProgress == 100
                    ) {

                        appendLog(
                            "[PROGRESS] 100%"
                        )
                    }

                    super.onProgressChanged(
                        view,
                        newProgress
                    )
                }
            }
    }

    private fun injectJavaScriptHooks(
        view: WebView?
    ) {

        if (view == null) {
            return
        }

        val script = """
            (function() {

                try {

                    if (
                        window.__VALE_ANALYZER_INSTALLED
                    ) {
                        return;
                    }

                    window.__VALE_ANALYZER_INSTALLED = true;

                    function send(type, value) {

                        try {

                            if (
                                window.AnalyzerBridge
                            ) {

                                window.AnalyzerBridge.log(
                                    "[" + type + "] " +
                                    String(value)
                                );
                            }

                        } catch(e) {}
                    }

                    send(
                        "JS",
                        "Hook instalado"
                    );

                    send(
                        "JS",
                        "URL=" + location.href
                    );

                    send(
                        "JS",
                        "TITLE=" + document.title
                    );

                    send(
                        "JS",
                        "READY=" + document.readyState
                    );

                    send(
                        "JS",
                        "LANG=" + navigator.language
                    );

                    send(
                        "JS",
                        "UA=" + navigator.userAgent
                    );

                    send(
                        "JS",
                        "DPR=" + window.devicePixelRatio
                    );

                    send(
                        "JS",
                        "VIEWPORT=" +
                        window.innerWidth +
                        "x" +
                        window.innerHeight
                    );

                    send(
                        "DOM",
                        "HTML_LENGTH=" +
                        document.documentElement
                            .outerHTML.length
                    );

                    send(
                        "DOM",
                        "SCRIPTS=" +
                        document.scripts.length
                    );

                    send(
                        "DOM",
                        "LINKS=" +
                        document.querySelectorAll(
                            "link"
                        ).length
                    );

                    send(
                        "DOM",
                        "IMAGES=" +
                        document.images.length
                    );

                    send(
                        "DOM",
                        "VIDEOS=" +
                        document.querySelectorAll(
                            "video"
                        ).length
                    );

                    send(
                        "DOM",
                        "AUDIOS=" +
                        document.querySelectorAll(
                            "audio"
                        ).length
                    );

                    var originalFetch =
                        window.fetch;

                    if (
                        originalFetch
                    ) {

                        window.fetch =
                            function() {

                                try {

                                    var input =
                                        arguments[0];

                                    var url =
                                        typeof input ===
                                        "string"
                                        ? input
                                        : (
                                            input &&
                                            input.url
                                        );

                                    send(
                                        "FETCH",
                                        url ||
                                        "(unknown)"
                                    );

                                } catch(e) {

                                    send(
                                        "FETCH_ERROR",
                                        e
                                    );
                                }

                                return originalFetch
                                    .apply(
                                        this,
                                        arguments
                                    );
                            };
                    }

                    var originalOpen =
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
                                    method +
                                    " " +
                                    url
                                );

                            } catch(e) {}

                            return originalOpen
                                .apply(
                                    this,
                                    arguments
                                );
                        };

                    var OriginalWebSocket =
                        window.WebSocket;

                    if (
                        OriginalWebSocket
                    ) {

                        window.WebSocket =
                            function(
                                url,
                                protocols
                            ) {

                                try {

                                    send(
                                        "WEBSOCKET",
                                        url
                                    );

                                } catch(e) {}

                                if (
                                    protocols !==
                                    undefined
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

                        window.WebSocket
                            .prototype =
                            OriginalWebSocket
                                .prototype;
                    }

                    var originalConsoleLog =
                        console.log;

                    console.log =
                        function() {

                            try {

                                var parts = [];

                                for (
                                    var i = 0;
                                    i < arguments.length;
                                    i++
                                ) {

                                    parts.push(
                                        String(
                                            arguments[i]
                                        )
                                    );
                                }

                                send(
                                    "CONSOLE_LOG",
                                    parts.join(" ")
                                );

                            } catch(e) {}

                            return originalConsoleLog
                                .apply(
                                    console,
                                    arguments
                                );
                        };

                    var originalConsoleError =
                        console.error;

                    console.error =
                        function() {

                            try {

                                var parts = [];

                                for (
                                    var i = 0;
                                    i < arguments.length;
                                    i++
                                ) {

                                    parts.push(
                                        String(
                                            arguments[i]
                                        )
                                    );
                                }

                                send(
                                    "CONSOLE_ERROR",
                                    parts.join(" ")
                                );

                            } catch(e) {}

                            return originalConsoleError
                                .apply(
                                    console,
                                    arguments
                                );
                        };

                } catch(e) {

                    try {

                        if (
                            window.AnalyzerBridge
                        ) {

                            window.AnalyzerBridge.log(
                                "[HOOK_ERROR] " +
                                String(e)
                            );
                        }

                    } catch(ignore) {}
                }

            })();
        """.trimIndent()

        view.evaluateJavascript(
            script,
            null
        )
    }

    private fun classifyRequest(
        url: String,
        headers: Map<String, String>
    ): String {

        val lower =
            url.lowercase(
                Locale.ROOT
            )

        val accept =
            headers["Accept"]
                ?.lowercase(
                    Locale.ROOT
                )
                ?: ""

        return when {

            lower.contains(".js") ||
                    accept.contains(
                        "javascript"
                    ) ->
                "JS"

            lower.contains(".css") ||
                    accept.contains(
                        "text/css"
                    ) ->
                "CSS"

            lower.contains(".json") ||
                    accept.contains(
                        "application/json"
                    ) ->
                "JSON"

            lower.contains(".wasm") ||
                    accept.contains(
                        "application/wasm"
                    ) ->
                "WASM"

            lower.contains(".png") ||
                    lower.contains(".jpg") ||
                    lower.contains(".jpeg") ||
                    lower.contains(".gif") ||
                    lower.contains(".webp") ||
                    lower.contains(".svg") ||
                    accept.startsWith(
                        "image/"
                    ) ->
                "IMAGE"

            lower.contains(".mp3") ||
                    lower.contains(".wav") ||
                    lower.contains(".ogg") ||
                    lower.contains(".m4a") ||
                    accept.startsWith(
                        "audio/"
                    ) ->
                "AUDIO"

            lower.contains(".mp4") ||
                    lower.contains(".webm") ||
                    lower.contains(".mov") ||
                    accept.startsWith(
                        "video/"
                    ) ->
                "VIDEO"

            lower.contains(".woff") ||
                    lower.contains(".woff2") ||
                    lower.contains(".ttf") ||
                    lower.contains(".otf") ||
                    accept.contains(
                        "font/"
                    ) ->
                "FONT"

            lower.contains("/api/") ||
                    lower.contains("/api?") ||
                    lower.contains(
                        "graphql"
                    ) ||
                    lower.contains(
                        "/rpc/"
                    ) ->
                "API"

            lower.startsWith(
                "data:"
            ) ->
                "DATA"

            else ->
                "OTHER"
        }
    }

    private fun getCookieInfo(
        url: String?
    ): String {

        if (
            url.isNullOrBlank()
        ) {
            return "0"
        }

        return try {

            val cookie =
                android.webkit.CookieManager
                    .getInstance()
                    .getCookie(url)

            cookie ?: "(nenhum)"

        } catch (e: Exception) {

            "(erro: ${e.message})"
        }
    }

    private fun copyLogs() {

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        val text =
            buildReport()

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vale Herança Analyzer",
                text
            )
        )

        saveStatus =
            "LOG COPIADO"

        appendLog(
            "[UI] Relatório copiado."
        )
    }

    private fun buildReport(): String {

        val now =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.US
            ).format(Date())

        return buildString {

            appendLine(
                "=================================================="
            )

            appendLine(
                "VALE HERANÇA ANALYZER"
            )

            appendLine(
                "RELATÓRIO COMPLETO"
            )

            appendLine(
                "=================================================="
            )

            appendLine(
                "Data: $now"
            )

            appendLine(
                "Android SDK: ${Build.VERSION.SDK_INT}"
            )

            appendLine(
                "Fabricante: ${Build.MANUFACTURER}"
            )

            appendLine(
                "Modelo: ${Build.MODEL}"
            )

            appendLine(
                "Pacote: com.gontijotech.valeheranca"
            )

            appendLine()

            appendLine(
                "GAME URL:"
            )

            appendLine(
                GAME_URL
            )

            appendLine()

            appendLine(
                "ÚLTIMA URL:"
            )

            appendLine(
                lastUrl
            )

            appendLine()

            appendLine(
                "TOTAL DE REQUESTS:"
            )

            appendLine(
                requestCounter.get().toString()
            )

            appendLine()

            appendLine(
                "=================================================="
            )

            appendLine(
                "LOG"
            )

            appendLine(
                "=================================================="
            )

            logs.forEach { line ->

                appendLine(line)
            }
        }
    }

    private fun saveReportToDownloads() {

        try {

            saveStatus =
                "SALVANDO..."

            val resolver =
                contentResolver

            val collection =
                MediaStore.Downloads
                    .EXTERNAL_CONTENT_URI

            var existingUri: Uri? = null

            resolver.query(
                collection,
                arrayOf(
                    MediaStore.Downloads._ID
                ),
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(
                    REPORT_FILE_NAME
                ),
                null
            )?.use { cursor ->

                if (
                    cursor.moveToFirst()
                ) {

                    val id =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID
                            )
                        )

                    existingUri =
                        Uri.withAppendedPath(
                            collection,
                            id.toString()
                        )
                }
            }

            val uri =
                existingUri
                    ?: createDownloadFile(
                        resolver,
                        collection
                    )

            if (uri == null) {

                saveStatus =
                    "ERRO AO CRIAR ARQUIVO"

                appendLog(
                    "[SAVE ERROR] MediaStore retornou URI nula."
                )

                return
            }

            resolver
                .openOutputStream(
                    uri,
                    "wt"
                )
                ?.use { output ->

                    output.write(
                        buildReport()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )

                    output.flush()
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    values,
                    null,
                    null
                )
            }

            saveStatus =
                "SALVO EM DOWNLOADS"

            appendLog(
                "[SAVE] Relatório salvo: Downloads/$REPORT_FILE_NAME"
            )

        } catch (e: Exception) {

            saveStatus =
                "ERRO: ${e.message}"

            appendLog(
                "[SAVE ERROR] " +
                        "${e.javaClass.simpleName}: " +
                        "${e.message}"
            )
        }
    }

    private fun createDownloadFile(
        resolver: android.content.ContentResolver,
        collection: Uri
    ): Uri? {

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

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "Download/"
                    )

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        1
                    )
                }
            }

        return resolver.insert(
            collection,
            values
        )
    }

    override fun onResume() {

        super.onResume()

        webViewInstance?.onResume()
    }

    override fun onPause() {

        webViewInstance?.onPause()

        super.onPause()
    }

    override fun onDestroy() {

        webViewInstance?.apply {

            stopLoading()

            removeJavascriptInterface(
                "AnalyzerBridge"
            )

            webChromeClient = null

            webViewClient = null

            destroy()
        }

        webViewInstance = null

        super.onDestroy()
    }
}

@Composable
fun ControlBar(
    showConsole: Boolean,
    saveStatus: String,
    onToggleConsole: () -> Unit,
    onReload: () -> Unit,
    onClearLogs: () -> Unit,
    onCopyLogs: () -> Unit,
    onSaveLogs: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF1E1E1E)
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    "Recarregar",
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
                    if (showConsole)
                        "Ocultar Log"
                    else
                        "Ver Log",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            OutlinedButton(
                onClick = onCopyLogs,
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    "COPIAR",
                    fontSize = 10.sp
                )
            }

            OutlinedButton(
                onClick = onClearLogs,
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    "LIMPAR",
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = onSaveLogs,
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFE50914)
                    )
            ) {

                Text(
                    "SALVAR TXT",
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }

        if (saveStatus.isNotBlank()) {

            Text(
                text = saveStatus,
                color = if (
                    saveStatus.startsWith(
                        "SALVO"
                    )
                ) {
                    Color(0xFF35C759)
                } else {
                    Color(0xFFFFCC00)
                },
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 3.dp
                    )
            )
        }
    }
}

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

        items(logs) { log ->

            Text(
                text = log,

                color = when {

                    log.contains(
                        "[CONSOLE]"
                    ) ->
                        Color(0xFF4EC9B0)

                    log.contains(
                        "[PAGE]"
                    ) ->
                        Color(0xFFCE9178)

                    log.contains(
                        "[DATA]"
                    ) ->
                        Color(0xFFDCDCAA)

                    log.contains(
                        "[REQ"
                    ) ->
                        Color(0xFF9CDCFE)

                    log.contains(
                        "[FETCH]"
                    ) ->
                        Color(0xFFC586C0)

                    log.contains(
                        "[WEBSOCKET]"
                    ) ->
                        Color(0xFFFFCC00)

                    log.contains(
                        "[HTTP ERROR]"
                    ) ->
                        Color(0xFFFF5555)

                    else ->
                        Color(0xFFCCCCCC)
                },

                fontSize = 10.sp,

                fontFamily =
                    FontFamily.Monospace,

                lineHeight = 13.sp,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 1.dp
                    )
            )
        }
    }
}
