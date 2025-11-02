package com.heckpet.androeasy.ui

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.heckpet.androeasy.RootShell

@Composable
fun MainScreen() {
    var status by remember { mutableStateOf("Сервер запущен") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("AndroEasy", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val out = RootShell.exec("id")
            status = if (out.contains("uid=0")) "Root: OK" else "Root: НЕТ"
        }) {
            Text("Проверить Root")
        }

        Button(onClick = {
            RootShell.exec("mount -o remount,rw /")
            status = "Система RW"
        }) {
            Text("Разрешить запись в /")
        }

        val context = LocalContext.current
        Button(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://127.0.0.1:8443"))
            context.startActivity(intent)
        }) {
            Text("Открыть в браузере")
        }

        Text(status, color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.proceed() // ПРИНИМАЕМ САМОПОДПИСАННЫЙ СЕРТИФИКАТ!
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        loadUrl("https://127.0.0.1:8443")
                    }
                },
                modifier = Modifier.height(500.dp)
            )
        }
    }
}