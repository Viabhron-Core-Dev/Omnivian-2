package com.example.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.engine.db.AppDatabase
import com.example.engine.db.ChatSettingsEntity
import com.example.utils.LogKeeper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsBottomSheet(
    workspaceId: String,
    currentModel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.chatSettingsDao()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var temperature by remember { mutableStateOf(0.7f) }
    var minP by remember { mutableStateOf(0.05f) }
    var topP by remember { mutableStateOf(0.95f) }
    var maxTokens by remember { mutableStateOf(2048) }
    var systemPrompt by remember { mutableStateOf("") }
    var contextSize by remember { mutableStateOf(2048) }
    var numThreads by remember { mutableStateOf(4) }
    var useMmap by remember { mutableStateOf(true) }
    var useMlock by remember { mutableStateOf(false) }
    var unfoldOnScreen by remember { mutableStateOf(false) }

    val isLocalAi = currentModel.endsWith(".gguf", ignoreCase = true) || currentModel.startsWith("local_gguf/")

    LaunchedEffect(workspaceId) {
        val existing = dao.getSettings(workspaceId)
        if (existing != null) {
            temperature = existing.temperature
            minP = existing.minP
            topP = existing.topP
            maxTokens = existing.maxTokens
            systemPrompt = existing.systemPrompt
            contextSize = existing.contextSize
            numThreads = existing.numThreads
            useMmap = existing.useMmap
            useMlock = existing.useMlock
            unfoldOnScreen = existing.unfoldOnScreen
            LogKeeper.log("ChatSettings", "Loaded", "Loaded thread settings for workspace $workspaceId (temp=$temperature, unfoldOnScreen=$unfoldOnScreen)")
        }
    }

    fun persistSettings() {
        scope.launch {
            val entity = ChatSettingsEntity(
                workspaceId = workspaceId,
                temperature = temperature,
                minP = minP,
                topP = topP,
                maxTokens = maxTokens,
                systemPrompt = systemPrompt,
                contextSize = contextSize,
                numThreads = numThreads,
                useMmap = useMmap,
                useMlock = useMlock,
                unfoldOnScreen = unfoldOnScreen
            )
            dao.saveSettings(entity)
            LogKeeper.log("ChatSettings", "Sync", "Persisted settings for workspace $workspaceId (temp=$temperature, topP=$topP, unfoldOnScreen=$unfoldOnScreen)")
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            persistSettings()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Model & Chat Controls", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (isLocalAi) "Local AI (GGUF / llama.cpp)" else "Cloud API Model ($currentModel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    persistSettings()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            }

            // Quick Preset Profiles
            Text("Inference Presets", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                FilterChip(
                    selected = (temperature == 0.2f && topP == 0.85f),
                    onClick = {
                        temperature = 0.2f
                        topP = 0.85f
                        minP = 0.08f
                        persistSettings()
                        LogKeeper.log("ChatSettings", "Preset", "Applied 'Precise / Code' preset to $workspaceId")
                    },
                    label = { Text("🎯 Precise") }
                )
                FilterChip(
                    selected = (temperature == 0.7f && topP == 0.95f),
                    onClick = {
                        temperature = 0.7f
                        topP = 0.95f
                        minP = 0.05f
                        persistSettings()
                        LogKeeper.log("ChatSettings", "Preset", "Applied 'Balanced' preset to $workspaceId")
                    },
                    label = { Text("⚖️ Balanced") }
                )
                FilterChip(
                    selected = (temperature == 0.95f && topP == 0.98f),
                    onClick = {
                        temperature = 0.95f
                        topP = 0.98f
                        minP = 0.03f
                        persistSettings()
                        LogKeeper.log("ChatSettings", "Preset", "Applied 'Creative' preset to $workspaceId")
                    },
                    label = { Text("💡 Creative") }
                )
            }

            // System Prompt
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt / Persona") },
                placeholder = { Text("You are an expert AI software engineer...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                maxLines = 4
            )

            // Universal Controls: Temperature
            Text("Temperature: ${String.format("%.2f", temperature)}", style = MaterialTheme.typography.titleSmall)
            Text(
                "Controls randomness and creativity. Lower values produce more deterministic and focused answers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0.0f..2.0f,
                steps = 19,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Universal Controls: min-p
            Text("min-p Sampling: ${String.format("%.2f", minP)}", style = MaterialTheme.typography.titleSmall)
            Text(
                "Filters out tokens with probabilities lower than min-p times the top token probability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = minP,
                onValueChange = { minP = it },
                valueRange = 0.0f..0.5f,
                steps = 9,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Universal Controls: Top-P
            Text("Top-P (Nucleus): ${String.format("%.2f", topP)}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = topP,
                onValueChange = { topP = it },
                valueRange = 0.1f..1.0f,
                steps = 17,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Max Tokens
            Text("Max Output Tokens: $maxTokens", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = { maxTokens = (it / 256).roundToInt() * 256 },
                valueRange = 256f..4096f,
                steps = 14,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Mini-Phase 8.2: Thread Settings - Fold on Screen Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { unfoldOnScreen = !unfoldOnScreen }
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fold on Screen (Auto-Unfold Viewport)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (unfoldOnScreen) "Active: Messages automatically unfold as you scroll them into view."
                        else "Normal mode: Only the latest prompt and active reply stay unfolded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = unfoldOnScreen,
                    onCheckedChange = { unfoldOnScreen = it }
                )
            }

            // Local Hardware Section (Visible for Local AI)
            if (isLocalAi) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    "⚡ Local Hardware & Memory Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                // Context Size
                Text("Context Size (KV Cache): $contextSize tokens", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Allocates memory for chat history. 2048 is recommended for high TPS on mobile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(512, 1024, 2048, 4096).forEach { size ->
                        FilterChip(
                            selected = contextSize == size,
                            onClick = { contextSize = size },
                            label = { Text("$size") }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // CPU Threads
                Text("CPU Threads: $numThreads", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Number of CPU cores dedicated to matrix multiplication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = numThreads.toFloat(),
                    onValueChange = { numThreads = it.roundToInt() },
                    valueRange = 1f..8f,
                    steps = 6,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                // mmap / mlock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useMmap = !useMmap }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = useMmap, onCheckedChange = { useMmap = it })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Use mmap (Memory Mapping)", style = MaterialTheme.typography.bodyMedium)
                        Text("Maps model file directly into address space for faster load and less memory overhead.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useMlock = !useMlock }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = useMlock, onCheckedChange = { useMlock = it })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Use mlock (Lock in RAM)", style = MaterialTheme.typography.bodyMedium)
                        Text("Prevents OS from paging out model memory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
