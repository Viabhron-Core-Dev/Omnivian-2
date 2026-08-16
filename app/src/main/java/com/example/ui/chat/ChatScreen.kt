package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.utils.VoiceManager
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.UnfoldLess

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.omniroot.AiManagerViewModel
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import com.example.engine.omniroot.local.LocalAiManager
import com.example.utils.LogKeeper
import kotlinx.coroutines.flow.onCompletion
import com.example.engine.omniroot.local.LlamaEngine
import kotlinx.coroutines.flow.first
import com.example.engine.db.AppDatabase
import com.example.engine.db.toEntity
import com.example.engine.db.toDomainModel
import kotlinx.coroutines.launch
import com.example.engine.server.PreviewServerManager
import java.io.File
import java.util.UUID

enum class MessageRole {
    USER, AI, APP_ACTION, SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val role: MessageRole = MessageRole.USER,
    val modelName: String? = null,
    val providerId: String? = null,
    val editedFiles: List<Pair<String, Boolean>> = emptyList(),
    val appActions: List<String> = emptyList(),
    var isFolded: Boolean = true,
    var isRenderPaused: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onMenuClick: () -> Unit,
    onNavigateToThreadSettings: () -> Unit = {},
    onSessionPromoted: (String) -> Unit = {}
) {
    val aiViewModel: AiManagerViewModel = viewModel()
    val availableModels by aiViewModel.availableModels.collectAsState()
    
    val isTemporaryChat = remember(sessionId) { sessionId.startsWith("temp_") }
    val workspaceName = remember { mutableStateOf(com.example.engine.fs.LocalFileManager.getWorkspaceName(sessionId)) }
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isServerRunning by PreviewServerManager.isRunning.collectAsState()

    var showAgentSettings by remember { mutableStateOf(false) }
    var showTokenPanel by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var showArtifactsList by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var selectedModel by remember { mutableStateOf("Select Model") }
    
    val isListening by VoiceManager.isListening.collectAsState()
    val audioAmplitude by VoiceManager.amplitude.collectAsState()
    var recordedVoiceFile by remember { mutableStateOf<java.io.File?>(null) }
    
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            VoiceManager.startListening(
                context = context,
                onAudioRecorded = { file ->
                    recordedVoiceFile = file
                    val engine = VoiceManager.getSttEngine(context)
                    if (engine == VoiceManager.ENGINE_DIRECT_AUDIO) {
                        inputText = if (inputText.isBlank()) "[Audio: ${file.name}]" else "$inputText [Audio: ${file.name}]"
                        Toast.makeText(context, "Voice note recorded (${file.name})", Toast.LENGTH_SHORT).show()
                    }
                },
                onPartialResult = { partial ->
                    // Live partial result can be displayed or logged
                },
                onFinalResult = { text ->
                    if (text.isNotBlank()) {
                        inputText = if (inputText.isBlank()) text else "$inputText $text"
                        Toast.makeText(context, "Transcribed voice input", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val chatMessages = remember {
        mutableStateListOf<ChatMessage>()
    }
    val listState = rememberLazyListState()

    val db = AppDatabase.getDatabase(context)
    val dao = db.chatMessageDao()
    val settingsDao = db.chatSettingsDao()
    
    // Mini-Phase 8.2: Viewport Fold/Unfold on Screen state
    var isUnfoldOnScreenEnabled by remember { mutableStateOf(false) }

    // Mini-Phase 8.5: Live reactive settings synchronization with LogKeeper
    LaunchedEffect(sessionId) {
        settingsDao.getSettingsFlow(sessionId).collect { settings ->
            if (settings != null) {
                isUnfoldOnScreenEnabled = settings.unfoldOnScreen
                LogKeeper.log("ChatSettings", "FlowSync", "Reactive sync for $sessionId: unfoldOnScreen=${settings.unfoldOnScreen}")
            }
        }
    }

    // Mini-Phase 8.2: Scroll observer for Fold on Screen
    LaunchedEffect(isUnfoldOnScreenEnabled, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (isUnfoldOnScreenEnabled && chatMessages.isNotEmpty()) {
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
            visibleIndices.forEach { idx ->
                if (idx in chatMessages.indices && chatMessages[idx].isFolded) {
                    chatMessages[idx] = chatMessages[idx].copy(isFolded = false)
                }
            }
        }
    }

    // Mini-Phase 8.4: Auto-scroll to latest message when new turns arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    LaunchedEffect(sessionId) {
        workspaceName.value = com.example.engine.fs.LocalFileManager.getWorkspaceName(sessionId)
        val initialMessages = dao.getMessagesForSession(sessionId).first()
        chatMessages.clear()
        val domainMessages = initialMessages.map { it.toDomainModel() }
        if (domainMessages.isNotEmpty()) {
            // Check if all messages are folded (e.g. first load or all collapsed)
            val hasExplicitFolds = initialMessages.any { !it.isFolded }
            if (hasExplicitFolds) {
                // Restore exact persisted user fold states
                domainMessages.forEach { msg ->
                    chatMessages.add(msg)
                }
            } else {
                // Active Turn Rule fallback: Keep latest active turn unfolded
                val lastUserIdx = domainMessages.indexOfLast { it.role == MessageRole.USER }
                val activeTurnStart = if (lastUserIdx != -1) lastUserIdx else (domainMessages.size - 1)
                
                domainMessages.forEachIndexed { index, msg ->
                    msg.isFolded = (index < activeTurnStart)
                    chatMessages.add(msg)
                }
            }
        }
    }


    
    fun saveMessage(msg: ChatMessage) {
        if (msg.text != "Thinking...") {
            scope.launch { 
                dao.insertMessage(msg.toEntity(sessionId))
                LogKeeper.log("ChatScreen", "MessagePersisted", "Saved ${msg.role} message (${msg.id}) in session $sessionId")
            }
        }
    }

    fun toggleMessageFold(index: Int) {
        if (index in chatMessages.indices) {
            val updated = chatMessages[index].copy(isFolded = !chatMessages[index].isFolded)
            chatMessages[index] = updated
            scope.launch {
                dao.updateFoldState(updated.id, updated.isFolded)
                LogKeeper.log("ChatScreen", "FoldToggled", "Toggled fold state for msg ${updated.id} to isFolded=${updated.isFolded}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showAgentSettings = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(workspaceName.value, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Agent Settings", modifier = Modifier.size(16.dp))
                }
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Menu")
                }
            },
            actions = {
                var showMenu by remember { mutableStateOf(false) }
                var showRename by remember { mutableStateOf(false) }

                if (isTemporaryChat) {
                    IconButton(onClick = {
                        val permanentId = java.util.UUID.randomUUID().toString()
                        val newTitle = if (workspaceName.value.startsWith("🔥")) workspaceName.value.removePrefix("🔥").trim() else workspaceName.value
                        com.example.engine.fs.LocalFileManager.setWorkspaceName(permanentId, newTitle.ifBlank { "Saved Chat" })
                        
                        scope.launch {
                            chatMessages.forEach { msg ->
                                dao.insertMessage(msg.toEntity(permanentId))
                            }
                            LogKeeper.log("ChatScreen", "PromotedToWorkspace", "Temporary chat $sessionId promoted to permanent $permanentId")
                            Toast.makeText(context, "Saved as permanent workspace chat!", Toast.LENGTH_SHORT).show()
                            onSessionPromoted(permanentId)
                        }
                    }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Save to Workspace",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = {
                    showArtifactsList = true
                }) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "Artifacts"
                    )
                }
                
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Fold All") },
                        leadingIcon = { Icon(Icons.Default.UnfoldLess, contentDescription = null) },
                        onClick = { 
                            showMenu = false
                            chatMessages.indices.forEach { i ->
                                val folded = chatMessages[i].copy(isFolded = true)
                                chatMessages[i] = folded
                                scope.launch { dao.updateFoldState(folded.id, true) }
                            }
                            Toast.makeText(context, "All messages folded", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Unfold All") },
                        leadingIcon = { Icon(Icons.Default.UnfoldMore, contentDescription = null) },
                        onClick = { 
                            showMenu = false
                            chatMessages.indices.forEach { i ->
                                val unfolded = chatMessages[i].copy(isFolded = false, isRenderPaused = true)
                                chatMessages[i] = unfolded
                                scope.launch { dao.updateFoldState(unfolded.id, false) }
                            }
                            Toast.makeText(context, "All messages unfolded", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRename = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive (GDrive)") },
                        onClick = { 
                            showMenu = false
                            android.widget.Toast.makeText(context, "Archive requires Google Drive integration", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false }
                    )
                }

                if (showRename) {
                    var newName by remember { mutableStateOf(workspaceName.value) }
                    AlertDialog(
                        onDismissRequest = { showRename = false },
                        title = { Text("Rename Chat") },
                        text = { 
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Chat Name") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                workspaceName.value = newName
                                com.example.engine.fs.LocalFileManager.setWorkspaceName(sessionId, newName)
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val config = db.workspaceConfigDao().getConfig(sessionId)
                                    if (config != null) {
                                        db.workspaceConfigDao().saveConfig(config.copy(threadName = newName))
                                    }
                                }
                                showRename = false
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRename = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatMessages.size, key = { chatMessages[it].id }) { index ->
                val message = chatMessages[index]
                when (message.role) {
                    MessageRole.USER -> UserMessage(
                        message = message,
                        onToggleFold = { toggleMessageFold(index) }
                    )
                    MessageRole.AI -> AiMessage(
                        message = message,
                        aiViewModel = aiViewModel,
                        onToggleFold = { toggleMessageFold(index) }
                    )
                    MessageRole.APP_ACTION -> AppActionMessage(
                        message = message,
                        onFileClick = { selectedFile = it },
                        onToggleFold = { toggleMessageFold(index) }
                    )
                    MessageRole.SYSTEM -> { /* System messages are not rendered in user chat timeline */ }
                }
            }
        }

        // Chat Input Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Make changes, add new features, ask for anything") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = Int.MAX_VALUE
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Agent/Model Selector Pill
                    var showModelPicker by remember { mutableStateOf(false) }
                    
                    // Update selected model if it's not in the available models list
                    LaunchedEffect(availableModels) {
                        if (availableModels.isNotEmpty() && !availableModels.contains(selectedModel)) {
                            selectedModel = availableModels.first()
                        }
                    }
                    
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable { showModelPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayInitials = remember(selectedModel) {
                                    if (selectedModel == "Select Model" || selectedModel.startsWith("No models") || selectedModel.startsWith("Loading")) {
                                        "AI"
                                    } else if (selectedModel.contains("/")) {
                                        val parts = selectedModel.split("/", limit = 2)
                                        val p = parts[0].firstOrNull()?.uppercaseChar() ?: '?'
                                        val m = parts[1].firstOrNull()?.uppercaseChar() ?: '?'
                                        "$p / $m"
                                    } else {
                                        selectedModel.take(2).uppercase()
                                    }
                                }
                                Text(displayInitials, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showModelPicker,
                            onDismissRequest = { showModelPicker = false }
                        ) {
                            availableModels.forEach { modelName ->
                                DropdownMenuItem(
                                    text = { Text(modelName) },
                                    onClick = { 
                                        selectedModel = modelName
                                        showModelPicker = false 
                                    }
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { showAttachmentPicker = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (isListening) {
                                    VoiceManager.stopListening { file ->
                                        val engine = VoiceManager.getSttEngine(context)
                                        if (engine == VoiceManager.ENGINE_DIRECT_AUDIO) {
                                            recordedVoiceFile = file
                                            inputText = if (inputText.isBlank()) "[Audio: ${file.name}]" else "$inputText [Audio: ${file.name}]"
                                            Toast.makeText(context, "Recorded: ${file.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop Recording" else "Voice Recording",
                                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (isGenerating) {
                                    currentJob?.cancel()
                                    isGenerating = false
                                } else if (inputText.isNotBlank()) {
                                    val prompt = inputText
                                    
                                    // Mini-Phase 8.4: Auto-fold all previous messages and persist for active turn isolation
                                    chatMessages.indices.forEach { i ->
                                        if (!chatMessages[i].isFolded) {
                                            val folded = chatMessages[i].copy(isFolded = true)
                                            chatMessages[i] = folded
                                            scope.launch { dao.updateFoldState(folded.id, true) }
                                        }
                                    }
                                    
                                    val msg = ChatMessage(text = prompt, role = MessageRole.USER, isFolded = false)
                                    chatMessages.add(msg)
                                    saveMessage(msg)
                                    inputText = ""
                                    

                                    val parts = selectedModel.split("/", limit = 2)
                                    var currentProvider = parts.getOrNull(0)
                                    var currentModel = parts.getOrNull(1) ?: selectedModel
                                    
                                    if (currentProvider == "Select Model" || currentProvider == null) {
                                        currentProvider = "google_ai_studio"
                                        currentModel = "gemini-1.5-pro-latest"
                                    }

                                    val loadingText = "Thinking..."
                                    val generatingMessage = ChatMessage(text = loadingText, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider, isFolded = false)

                                    chatMessages.add(generatingMessage)
                                    
                                    isGenerating = true
                                    currentJob = scope.launch {
                                        try {
                                            val currentSettings = db.chatSettingsDao().getSettings(sessionId)
                                            val temperature = currentSettings?.temperature ?: 0.7f
                                            val minP = currentSettings?.minP ?: 0.05f
                                            val topP = currentSettings?.topP ?: 0.95f
                                            val maxTokens = currentSettings?.maxTokens ?: 2048
                                            val systemPrompt = currentSettings?.systemPrompt ?: ""
                                            val contextSize = currentSettings?.contextSize ?: 2048
                                            val numThreads = currentSettings?.numThreads ?: 4
                                            val useMmap = currentSettings?.useMmap ?: true
                                            val useMlock = currentSettings?.useMlock ?: false

                                            if (currentProvider == "local_gguf") {
                                                // Mini-Phase 3 & 4: Direct Bypass and Streaming UI with SmolChat hardware engine
                                                val models = db.aiModelDao().getAllModels().first()
                                                val modelEntity = models.firstOrNull { it.providerId == "local_gguf" && it.modelId == currentModel }
                                                val absolutePath = modelEntity?.description ?: currentModel
                                                
                                                val llama = LocalAiManager.getOrLoadEngine(
                                                    context, 
                                                    absolutePath,
                                                    contextSize = contextSize,
                                                    numThreads = numThreads,
                                                    useMmap = useMmap,
                                                    useMlock = useMlock
                                                )
                                                
                                                if (llama != null) {
                                                    var combinedPrompt = ""
                                                    if (systemPrompt.isNotBlank()) {
                                                        combinedPrompt += "<|im_start|>system\n$systemPrompt<|im_end|>\n"
                                                    }
                                                    chatMessages.filter { it.id != generatingMessage.id }.forEach { msg ->
                                                        val roleStr = if (msg.role == MessageRole.USER) "user" else "assistant"
                                                        combinedPrompt += "<|im_start|>$roleStr\n${msg.text}<|im_end|>\n"
                                                    }
                                                    combinedPrompt += "<|im_start|>assistant\n"
                                                    
                                                    var streamedText = ""
                                                    val startTime = System.currentTimeMillis()
                                                    var tokenCount = 0
                                                    
                                                    llama.predictFlow(
                                                        prompt = combinedPrompt,
                                                        temperature = temperature,
                                                        minP = minP,
                                                        topP = topP,
                                                        maxTokens = maxTokens
                                                    ).collect { token ->
                                                        streamedText += token
                                                        tokenCount++
                                                        
                                                        val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                        if (index != -1) {
                                                            chatMessages[index] = generatingMessage.copy(text = streamedText)
                                                        }
                                                    }
                                                    
                                                    val endTime = System.currentTimeMillis()
                                                    val elapsedSec = (endTime - startTime) / 1000.0
                                                    val tps = if (elapsedSec > 0) tokenCount / elapsedSec else 0.0
                                                    LogKeeper.log("Local AI", "Metrics", "Stream finished. Tokens: $tokenCount, Time: ${elapsedSec}s, TPS: $tps")
                                                    
                                                    // Final save
                                                    val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                    if (index != -1) {
                                                        val finalMsg = chatMessages[index]
                                                        saveMessage(finalMsg)
                                                    }
                                                } else {
                                                    val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                    if (index != -1) {
                                                        val finalMsg = generatingMessage.copy(text = "Error: Local model failed to load (OOM or File Not Found).")
                                                        chatMessages[index] = finalMsg
                                                        saveMessage(finalMsg)
                                                    }
                                                }
                                                
                                            } else {
                                                // Normal OmniRoot HTTP Proxy flow with custom parameters
                                                val messagesToSend = mutableListOf<ChatMessage>()
                                                if (systemPrompt.isNotBlank()) {
                                                    messagesToSend.add(ChatMessage(text = systemPrompt, role = MessageRole.SYSTEM))
                                                }
                                                messagesToSend.addAll(chatMessages.filter { it.id != generatingMessage.id })

                                                val response = com.example.ui.chat.OmniRootClient.generateContent(
                                                    messages = messagesToSend,
                                                    model = selectedModel,
                                                    temperature = temperature,
                                                    topP = topP,
                                                    maxTokens = maxTokens
                                                )
                                                val index = chatMessages.indexOf(generatingMessage)
                                                if (index != -1) {
                                                    chatMessages.removeAt(index)
                                                }
                                                
                                                if (response.actions.isNotEmpty() || response.editedFiles.isNotEmpty()) {
                                                    val msg = ChatMessage(text = "", role = MessageRole.APP_ACTION, appActions = response.actions, editedFiles = response.editedFiles, isFolded = false)
                                                    chatMessages.add(msg)
                                                    saveMessage(msg)
                                                }

                                                if (!response.text.isNullOrBlank()) {
                                                    val msg = ChatMessage(text = response.text, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider, isFolded = false)

                                                    chatMessages.add(msg)
                                                    saveMessage(msg)
                                                }
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                            if (index != -1) {
                                                val oldText = chatMessages[index].text
                                                val msg = chatMessages[index].copy(text = if (oldText.isBlank() || oldText.contains("Waking up")) "Generation stopped." else oldText)
                                                chatMessages[index] = msg
                                                saveMessage(msg)
                                            }
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isGenerating) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isGenerating) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            } else {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
        
        if (showAgentSettings) {
            AgentSettingsBottomSheet(
                workspaceId = sessionId,
                currentModel = selectedModel,
                onDismiss = { 
                    showAgentSettings = false
                    scope.launch {
                        val settings = settingsDao.getSettings(sessionId)
                        isUnfoldOnScreenEnabled = settings?.unfoldOnScreen ?: false
                    }
                }
            )
        }
        
        if (showTokenPanel) {
            AiTokenPanelBottomSheet(
                onDismiss = { showTokenPanel = false }
            )
        }
        
        selectedFile?.let { fileName ->
            FileAttachmentBottomSheet(
                fileName = fileName,
                onDismiss = { selectedFile = null }
            )
        }
        
        var selectedArtifact by remember { mutableStateOf<ArtifactItem?>(null) }
        
        if (showArtifactsList) {
            ArtifactsListBottomSheet(
                onDismiss = { showArtifactsList = false },
                onArtifactSelected = { artifact ->
                    selectedArtifact = artifact
                    showArtifactsList = false
                }
            )
        }
        
        selectedArtifact?.let { artifact ->
            PWAPreviewBottomSheet(
                url = artifact.url,
                title = artifact.name,
                onDismiss = { selectedArtifact = null }
            )
        }

        if (showAttachmentPicker) {
            AttachmentPickerBottomSheet(
                onDismiss = { showAttachmentPicker = false },
                onOptionSelected = { option ->
                    // Handle attachment logic here
                    when(option) {
                        is AttachmentOption.ImageUri -> {
                            val msg = ChatMessage(text = "Selected image: ${option.uri}", role = MessageRole.USER)
                            chatMessages.add(msg)
                            saveMessage(msg)
                        }
                        is AttachmentOption.FileUri -> {
                            val msg = ChatMessage(text = "Selected file: ${option.uri}", role = MessageRole.USER)
                            chatMessages.add(msg)
                            saveMessage(msg)
                            scope.launch {
                                val result = com.example.engine.fs.TextExtractor.extractTextFromUri(context, option.uri)
                                if (result.isSuccess) {
                                    val text = result.getOrNull()
                                    val msg = ChatMessage(text = "File content extracted (${text?.length} chars)", role = MessageRole.APP_ACTION)
                                    chatMessages.add(msg)
                                    saveMessage(msg)
                                } else {
                                    val msg = ChatMessage(text = "Failed to extract text", role = MessageRole.APP_ACTION)
                                    chatMessages.add(msg)
                                    saveMessage(msg)
                                }
                            }
                        }
                        is AttachmentOption.GithubRepo -> {
                            val msg = ChatMessage(text = "Importing repo: ${option.url} ...", role = MessageRole.USER)
                            chatMessages.add(msg)
                            saveMessage(msg)
                            scope.launch {
                                val repoName = option.url.trim().removeSuffix("/").substringAfterLast("/")
                                val destFolder = java.io.File(com.example.engine.fs.LocalFileManager.getWorkspaceDir(), repoName)
                                val destZip = java.io.File(com.example.engine.fs.LocalFileManager.getWorkspaceDir(), "$repoName.zip")
                                val result = com.example.engine.fs.GithubDownloader.downloadRepoAsZip(option.url, destZip)
                                if (result.isSuccess) {
                                    com.example.engine.fs.LocalFileManager.unzipFile(destZip, destFolder)
                                    destZip.delete()
                                    val msg = ChatMessage(text = "Successfully imported GitHub repo '$repoName' into workspace.", role = MessageRole.APP_ACTION)
                                    chatMessages.add(msg)
                                    saveMessage(msg)
                                } else {
                                    val msg = ChatMessage(text = "Failed to import repo: ${result.exceptionOrNull()?.message}", role = MessageRole.APP_ACTION)
                                    chatMessages.add(msg)
                                    saveMessage(msg)
                                }
                            }
                        }
                        is AttachmentOption.Workspace -> {
                            val msg = ChatMessage(text = "Workspace artifacts picker triggered", role = MessageRole.USER)
                            chatMessages.add(msg)
                            saveMessage(msg)
                        }
                        is AttachmentOption.GoogleDrive -> {
                            val msg = ChatMessage(text = "Google Drive picker triggered", role = MessageRole.USER)
                            chatMessages.add(msg)
                            saveMessage(msg)
                        }
                    }
                    showAttachmentPicker = false
                }
            )
        }
    }
}

@Composable
fun UserMessage(
    message: ChatMessage,
    onToggleFold: () -> Unit
) {
    val expanded = !message.isFolded
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "user_arrow_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleFold() }.padding(4.dp)
        ) {
            Text("You", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse message" else "Expand message",
                modifier = Modifier.size(16.dp).rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            // Folded Summary Badge for User prompt
            val previewText = if (message.text.length > 50) message.text.take(47) + "..." else message.text
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onToggleFold() }
            ) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun AiMessage(
    message: ChatMessage,
    aiViewModel: AiManagerViewModel,
    onToggleFold: () -> Unit
) {
    val context = LocalContext.current
    val expanded = !message.isFolded
    var userRating by remember(message.id) { mutableStateOf<Boolean?>(null) }
    
    val displayName = message.modelName ?: "Gemini Pro Latest"
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ai_arrow_anim"
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(end = 32.dp, start = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleFold() }.padding(4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse message" else "Expand message",
                modifier = Modifier.size(16.dp).rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Copy Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", message.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    // Read Aloud / TTS Action
                    val isSpeaking by VoiceManager.isSpeaking.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (isSpeaking) {
                                VoiceManager.stopSpeaking()
                            } else {
                                VoiceManager.speak(context, message.text)
                            }
                        }
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop" else "Listen",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSpeaking) "Stop" else "Listen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    // Ratings
                    if (message.modelName != null && message.providerId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (userRating != true) {
                                    userRating = true
                                    aiViewModel.rateModel(message.providerId, message.modelName, true, message.id)
                                    Toast.makeText(context, "Rated: Upvote", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(16.dp), tint = if (userRating == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (userRating != false) {
                                    userRating = false
                                    aiViewModel.rateModel(message.providerId, message.modelName, false, message.id)
                                    Toast.makeText(context, "Rated: Downvote", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = "Downvote", modifier = Modifier.size(16.dp), tint = if (userRating == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            // Folded Summary Badge for AI Response
            val charCount = message.text.length
            val tokenEst = charCount / 4
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.clickable { onToggleFold() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Response folded (~$tokenEst tokens • $charCount chars)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppActionMessage(
    message: ChatMessage,
    onFileClick: (String) -> Unit = {},
    onToggleFold: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(end = 32.dp, start = 8.dp)) {
        ActionHistoryCard(
            editedFiles = message.editedFiles,
            appActions = message.appActions,
            onFileClick = onFileClick,
            isFolded = message.isFolded,
            onToggleFold = onToggleFold
        )
    }
}

@Composable
fun ActionHistoryCard(
    modifier: Modifier = Modifier,
    editedFiles: List<Pair<String, Boolean>> = emptyList(),
    appActions: List<String> = emptyList(),
    onFileClick: (String) -> Unit = {},
    isFolded: Boolean = true,
    onToggleFold: () -> Unit = {}
) {
    val expanded = !isFolded
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "action_arrow_anim"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onToggleFold() }.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Action history",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Action Log (${appActions.size + editedFiles.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse actions" else "Expand actions",
                    modifier = Modifier.size(16.dp).rotate(arrowRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (appActions.isNotEmpty()) {
                        appActions.forEach { action ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = action,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (editedFiles.isNotEmpty()) {
                        // Edit section header
                        Text("Files edited:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        
                        editedFiles.forEach { (filePath, success) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .clickable { onFileClick(filePath) }
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (success) "Success" else "Failed",
                                    tint = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = filePath.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokenUsageBar(usedTokens: Int, maxTokens: Int) {
    val progress = usedTokens.toFloat() / maxTokens.toFloat()
    val isWarning = progress > 0.8f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(4.dp),
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$usedTokens / $maxTokens Tokens",
            style = MaterialTheme.typography.labelSmall,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
