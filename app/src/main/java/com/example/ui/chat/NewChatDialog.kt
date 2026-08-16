package com.example.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit
) {
    var threadName by remember { mutableStateOf("New Chat") }
    var appType by remember { mutableStateOf("Android Full Native") }
    var model by remember { mutableStateOf("Gemini Pro Latest") }
    var integrations by remember { mutableStateOf("Default Skills & Tools") }
    var instructions by remember { mutableStateOf("") }
    
    val appTypes = listOf("Android Full Native", "PWA (Web)")
    val models = listOf("Gemini Pro Latest", "Claude 3.5 Sonnet", "GPT-4o")
    
    var expandedAppType by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = threadName,
                    onValueChange = { threadName = it },
                    label = { Text("Chat Thread Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedAppType,
                    onExpandedChange = { expandedAppType = !expandedAppType }
                ) {
                    OutlinedTextField(
                        value = appType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Application Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAppType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAppType,
                        onDismissRequest = { expandedAppType = false }
                    ) {
                        appTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    appType = selectionOption
                                    expandedAppType = false
                                }
                            )
                        }
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = expandedModel,
                    onExpandedChange = { expandedModel = !expandedModel }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("AI Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModel,
                        onDismissRequest = { expandedModel = false }
                    ) {
                        models.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    model = selectionOption
                                    expandedModel = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = integrations,
                    onValueChange = { integrations = it },
                    label = { Text("Skills, Tools, MCP, Plugins") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("System Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(threadName, appType, model, integrations, instructions) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
