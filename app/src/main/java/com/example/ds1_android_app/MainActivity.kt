package com.example.ds1_android_app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "app_prefs"
    private val KEY_URL = "url"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            var currentUrl by remember { mutableStateOf(prefs.getString(KEY_URL, "") ?: "") }
            var showDialog by remember { mutableStateOf(false) }
            var inputUrl by remember { mutableStateOf(currentUrl) }

            // Column układa elementy od góry do dołu
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. NAJPIERW WEBVIEW / EKRAN STARTOWY (zajmuje całe wolne miejsce)
                if (currentUrl.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak ustawionego adresu.\nKliknij 'Ustawienia URL' na dole, aby dodać stronę.",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    var lastLoadedUrl by remember { mutableStateOf(currentUrl) }

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                loadUrl(currentUrl)
                            }
                        },
                        update = { webView ->
                            if (currentUrl != lastLoadedUrl) {
                                webView.loadUrl(currentUrl)
                                lastLoadedUrl = currentUrl
                            }
                        },
                        modifier = Modifier.weight(1f) // weight(1f) sprawia, że WebView wypycha przycisk na dół
                    )
                }

                // 2. NASTĘPNIE PRZYCISK USTAWIEŃ (na samym dole)
                Button(
                    onClick = {
                        inputUrl = currentUrl
                        showDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)  // Dodaje margines dookoła przycisku, żeby nie był przy samej krawędzi
                ) {
                    Text("⚙️ Ustawienia URL")
                }
            }

            // Okno dialogowe do wpisywania adresu (bez zmian)
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Adres strony (URL)") },
                    text = {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("np. wp.pl") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            var url = inputUrl.trim()
                            if (url.isNotEmpty() && !url.startsWith("http")) {
                                url = "https://$url"
                            }
                            prefs.edit().putString(KEY_URL, url).apply()
                            currentUrl = url
                            showDialog = false
                        }) {
                            Text("Zapisz")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }
    }
}