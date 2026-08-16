package com.example.ui.settings.omniroot

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectToKeyWebViewScreen(
    providerId: String,
    onNavigateBack: () -> Unit,
    viewModel: AiManagerViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val provider = providers.find { it.id == providerId }
    var pastedKey by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Get Key: ${provider?.name ?: "Unknown"}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (provider == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading provider details...")
                }
            } else {
                Text(
                    "We use Chrome Custom Tabs so you can securely log in with your existing browser sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Button(
                    onClick = {
                        val url = provider.loginUrl.takeIf { it.isNotEmpty() } ?: provider.baseUrl
                        val customTabsIntent = CustomTabsIntent.Builder().build()
                        customTabsIntent.launchUrl(context, Uri.parse(url))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open ${provider.name} Login Page")
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    "After generating your API key, return here to save it.",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias (e.g., Personal, Work)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = pastedKey,
                    onValueChange = { pastedKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        if (pastedKey.isNotBlank()) {
                            viewModel.saveRealKey(
                                providerId = providerId,
                                alias = alias.ifBlank { "${provider.name} Key" },
                                keyValue = pastedKey
                            )
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pastedKey.isNotBlank()
                ) {
                    Text("Save API Key")
                }
            }
        }
    }
}
