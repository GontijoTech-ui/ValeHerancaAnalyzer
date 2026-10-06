package com.gontijotech.valeheranca

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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

class AnalyzerBridge(private val onLog: (String) -> Unit) {
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
    private val gameUrl =
        "https://www.astrocade.com/games/vale-da-heran%C3%A7a-renova%C3%A7%C3%A3o/01M46K2HXGZ4F46MRCKS6WTQ6Y"

    fun appendLog(message: String) {
        runOnUiThread {
            logs.add(message)
        }
    }

    fun getLogs(): List<String> = logs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var showConsole by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        ControlBar(
                            showConsole = showConsole,
                            onToggleConsole = { showConsole = !showConsole },
                            onReload = { webViewInstance?.reload() },
                            onClearLogs = { logs.clear() }
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
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setBackgroundColor(AndroidColor.BLACK)
                                        configureWebView(this)
                                        addJavascriptInterface(
                                            AnalyzerBridge { appendLog(it) },
                                            "AnalyzerBridge"
                                        )
                                        loadUrl(gameUrl)
                                        webViewInstance = this
                                    }
                                }
                            )
                        }

                        if (showConsole) {
                            ConsoleView(
                                logs = logs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                appendLog("[PAGE] Iniciando: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                appendLog("[PAGE] Concluído: $url")
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    appendLog("[CONSOLE] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webViewInstance?.onResume()
    }

    override fun onPause() {
        super.onPause()
        webViewInstance?.onPause()
    }

    override fun onDestroy() {
        webViewInstance?.destroy()
        super.onDestroy()
    }
}

@Composable
fun ControlBar(
    showConsole: Boolean,
    onToggleConsole: () -> Unit,
    onReload: () -> Unit,
    onClearLogs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onReload,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Text("Recarregar", fontSize = 12.sp, color = Color.White)
        }

        Button(
            onClick = onToggleConsole,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showConsole) Color(0xFF007ACC) else Color(0xFF333333)
            )
        ) {
            Text(if (showConsole) "Ocultar Log" else "Ver Log", fontSize = 12.sp, color = Color.White)
        }

        if (showConsole) {
            Button(
                onClick = onClearLogs,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF552222))
            ) {
                Text("Limpar", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ConsoleView(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .padding(6.dp)
    ) {
        items(logs) { log ->
            Text(
                text = log,
                color = when {
                    log.contains("[CONSOLE]") -> Color(0xFF4EC9B0)
                    log.contains("[PAGE]") -> Color(0xFFCE9178)
                    log.contains("[DATA]") -> Color(0xFFDCDCAA)
                    else -> Color(0xFFCCCCCC)
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}
