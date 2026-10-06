package com.valeherancaanalyzer


import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val requestCounter =
        AtomicInteger(0)

    private val logLock =
        Any()

    private val logs =
        StringBuilder()

    private var logVersion by mutableStateOf(0)

    private var saveRunnable: Runnable? = null

    private var pageStartedAt =
        System.currentTimeMillis()

    private var lastPageUrl =
        GAME_URL

    private val analyzerBridge =
        AnalyzerBridge()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appendLog("==================================================")
        appendLog("VALE HERANÇA ANALYZER")
        appendLog("Inicialização do analisador")
        appendLog("Android SDK: ${Build.VERSION.SDK_INT}")
        appendLog("Modelo: ${Build.MODEL}")
        appendLog("Fabricante: ${Build.MANUFACTURER}")
        appendLog("==================================================")

        setContent {

            var liveLog by remember {
                mutableStateOf("")
            }

            var currentUrl by remember {
                mutableStateOf(GAME_URL)
            }

            DisposableEffect(logVersion) {

                liveLog = getLogs()

                onDispose {
                }
            }

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {

                        Text(
                            text = "VALE HERANÇA ANALYZER",
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                )
                        )

                        Text(
                            text = currentUrl,
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp
                                )
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            factory = { context ->

                                createWebView().also {
                                    webView = it
                                    it.loadUrl(GAME_URL)
                                }
                            },
                            update = { view ->

                                currentUrl =
                                    view.url ?: GAME_URL
                            }
                        )

                        ControlBar()

                        Text(
                            text = "LOG DO ANALISADOR",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(
                                    horizontal = 8.dp
                                )
                                .background(
                                    Color(0xFF080808)
                                )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(
                                        rememberScrollState()
                                    )
                                    .padding(8.dp)
                            ) {

                                Text(
                                    text = liveLog,
                                    color = Color(0xFF35C759),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {

        return WebView(this).apply {

            setBackgroundColor(Color.BLACK.value.toInt())

            settings.apply {

                javaScriptEnabled = true

                domStorageEnabled = true

                databaseEnabled = true

                allowFileAccess = true

                allowContentAccess = true

                loadsImagesAutomatically = true

                mediaPlaybackRequires
