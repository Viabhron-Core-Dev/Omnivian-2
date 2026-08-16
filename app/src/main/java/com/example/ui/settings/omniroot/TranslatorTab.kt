package com.example.ui.settings.omniroot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.engine.db.AppDatabase
import com.example.engine.db.FallbackChainEntity
import com.example.engine.omniroot.pipeline.TranslationEngine
import com.example.ui.chat.OmniRequest
import com.example.ui.chat.OmniMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorTab() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    val fallbackChains by db.fallbackChainDao().getAllChains().collectAsState(initial = emptyList())
    var showCreateChainDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Combo Routing (Fallback Chains)", style = MaterialTheme.typography.titleLarge)
            Text("Create dynamic routing chains. If the first provider fails or hits a rate limit, the proxy will instantly fallback to the next one in the chain.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showCreateChainDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Chain")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Fallback Chain")
            }
        }
        
        items(fallbackChains) { chain ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(chain.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val list = try {
                        val arr = JSONArray(chain.chainDataJson)
                        List(arr.length()) { arr.getString(it) }
                    } catch (e: Exception) { emptyList() }
                    
                    list.forEachIndexed { index, modelStr ->
                        Text("${index + 1}. $modelStr", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Payload Translation Playground", style = MaterialTheme.typography.titleLarge)
            Text("See how OmniRoot translates OpenAI-compatible payloads into native provider formats before sending them over the wire.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            TranslatorPlayground()
        }
    }

    if (showCreateChainDialog) {
        var chainName by remember { mutableStateOf("") }
        var provider1 by remember { mutableStateOf("google_ai_studio/gemini-1.5-pro-latest") }
        var provider2 by remember { mutableStateOf("groq/llama3-8b-8192") }
        
        AlertDialog(
            onDismissRequest = { showCreateChainDialog = false },
            title = { Text("New Fallback Chain") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chainName,
                        onValueChange = { chainName = it },
                        label = { Text("Chain Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provider1,
                        onValueChange = { provider1 = it },
                        label = { Text("Priority 1 Model") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provider2,
                        onValueChange = { provider2 = it },
                        label = { Text("Priority 2 Model") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val arr = JSONArray()
                    if (provider1.isNotBlank()) arr.put(provider1.trim())
                    if (provider2.isNotBlank()) arr.put(provider2.trim())
                    val json = arr.toString()
                    
                    scope.launch {
                        db.fallbackChainDao().insertChain(
                            FallbackChainEntity(
                                id = UUID.randomUUID().toString(),
                                name = chainName.ifBlank { "New Chain" },
                                chainDataJson = json,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        showCreateChainDialog = false
                    }
                }) {
                    Text("Save Chain")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChainDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorPlayground() {
    var inputJson by remember { mutableStateOf(
        """
        {
          "model": "gemini",
          "messages": [
            {"role": "system", "content": "You are a helpful assistant."},
            {"role": "user", "content": "Hello!"}
          ]
        }
        """.trimIndent()
    ) }
    
    var outputFormat by remember { mutableStateOf(TranslationEngine.ProviderFormat.ANTHROPIC) }
    var outputJson by remember { mutableStateOf("") }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = inputJson,
            onValueChange = { inputJson = it },
            label = { Text("OpenAI JSON Payload") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = outputFormat == TranslationEngine.ProviderFormat.ANTHROPIC,
                onClick = { outputFormat = TranslationEngine.ProviderFormat.ANTHROPIC },
                label = { Text("Anthropic Messages API") }
            )
            FilterChip(
                selected = outputFormat == TranslationEngine.ProviderFormat.GEMINI,
                onClick = { outputFormat = TranslationEngine.ProviderFormat.GEMINI },
                label = { Text("Gemini Native API") }
            )
        }
        
        Button(
            onClick = {
                try {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val req = moshi.adapter(OmniRequest::class.java).fromJson(inputJson)
                    if (req != null) {
                        outputJson = TranslationEngine.translateRequest(req, outputFormat)
                    } else {
                        outputJson = "Error: Could not parse request."
                    }
                } catch (e: Exception) {
                    outputJson = "Error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Translate, contentDescription = "Translate")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Translate Payload")
        }
        
        if (outputJson.isNotEmpty()) {
            OutlinedTextField(
                value = outputJson,
                onValueChange = {},
                readOnly = true,
                label = { Text("Translated Output") },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}
