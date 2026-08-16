package com.example.ui.chat

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PWAPreviewBottomSheet(
    url: String,
    title: String = "Artifact Preview",
    onDismiss: () -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Drag / Dismiss Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // Drag Indicator Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { onDismiss() }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                IconButton(onClick = { webView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                loadUrl(url)
                                webView = this
                            }
                        },
                        update = { view ->
                            // load URL
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
