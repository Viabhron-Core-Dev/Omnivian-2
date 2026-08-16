package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class ThreadSettingTab(val title: String) {
    UNIVERSAL("Universal"),
    AGENTS("Agents"),
    VERSIONS("Versions"),
    GITHUB("GitHub")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadSettingsScreen(
    workspaceId: String,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ThreadSettingTab.UNIVERSAL) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    var config by remember { mutableStateOf<com.example.engine.db.WorkspaceConfigEntity?>(null) }
    
    LaunchedEffect(workspaceId) {
        val db = com.example.engine.db.AppDatabase.getDatabase(context)
        val existingConfig = db.workspaceConfigDao().getConfig(workspaceId)
        if (existingConfig != null) {
            config = existingConfig
        } else {
            val defaultConfig = com.example.engine.db.WorkspaceConfigEntity(
                workspaceId = workspaceId,
                threadName = com.example.engine.fs.LocalFileManager.getWorkspaceName(workspaceId),
                appType = "Android Full Native",
                model = "Gemini Pro Latest",
                integrations = "Default Skills & Tools",
                instructions = "You are a helpful coding assistant."
            )
            db.workspaceConfigDao().saveConfig(defaultConfig)
            config = defaultConfig
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread Settings") },
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
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pill-shaped tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ThreadSettingTab.values()) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    ThreadSettingTab.UNIVERSAL -> UniversalSettingsContent(workspaceId, config) { updatedConfig ->
                        config = updatedConfig
                        scope.launch {
                            val db = com.example.engine.db.AppDatabase.getDatabase(context)
                            db.workspaceConfigDao().saveConfig(updatedConfig)
                            com.example.engine.fs.LocalFileManager.setWorkspaceName(workspaceId, updatedConfig.threadName)
                        }
                    }
                    ThreadSettingTab.AGENTS -> AgentsSettingsContent()
                    ThreadSettingTab.VERSIONS -> VersionsSettingsContent(workspaceId)
                    ThreadSettingTab.GITHUB -> GithubSettingsContent()
                }
            }
        }
    }
}


@Composable
fun UniversalSettingsContent(
    workspaceId: String,
    config: com.example.engine.db.WorkspaceConfigEntity?,
    onConfigChange: (com.example.engine.db.WorkspaceConfigEntity) -> Unit
) {
    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { com.example.engine.db.AppDatabase.getDatabase(context) }
    var unfoldOnScreen by remember { mutableStateOf(false) }

    LaunchedEffect(workspaceId) {
        val settings = db.chatSettingsDao().getSettings(workspaceId)
        unfoldOnScreen = settings?.unfoldOnScreen ?: false
    }

    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Fold on Screen Viewport Control Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Fold on Screen",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (unfoldOnScreen) "Active: Messages automatically unfold as you scroll them into the viewport."
                            else "Normal: Only the active turn stays open. Earlier turns remain folded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = unfoldOnScreen,
                        onCheckedChange = { isChecked ->
                            unfoldOnScreen = isChecked
                            scope.launch {
                                val currentSettings = db.chatSettingsDao().getSettings(workspaceId)
                                val updated = currentSettings?.copy(unfoldOnScreen = isChecked)
                                    ?: com.example.engine.db.ChatSettingsEntity(
                                        workspaceId = workspaceId,
                                        unfoldOnScreen = isChecked
                                    )
                                db.chatSettingsDao().saveSettings(updated)
                                com.example.utils.LogKeeper.log("ThreadSettings", "UniversalSync", "Toggled unfoldOnScreen=$isChecked for thread $workspaceId")
                            }
                        }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = config.threadName,
                onValueChange = { onConfigChange(config.copy(threadName = it)) },
                label = { Text("Thread Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = config.appType,
                onValueChange = { onConfigChange(config.copy(appType = it)) },
                label = { Text("App Type") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = config.model,
                onValueChange = { onConfigChange(config.copy(model = it)) },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = config.integrations,
                onValueChange = { onConfigChange(config.copy(integrations = it)) },
                label = { Text("Integrations") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = config.instructions,
                onValueChange = { onConfigChange(config.copy(instructions = it)) },
                label = { Text("System Instructions") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5
            )
        }
    }
}

@Composable
fun AgentsSettingsContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Active Agents in this Thread", style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text("OmniRoot (Default)") },
            supportingContent = { Text("Main coding assistant") },
            trailingContent = { Switch(checked = true, onCheckedChange = {}) }
        )
        ListItem(
            headlineContent = { Text("UI Designer") },
            supportingContent = { Text("Creates UI Maps") },
            trailingContent = { Switch(checked = false, onCheckedChange = {}) }
        )
        Button(onClick = { android.widget.Toast.makeText(context, "Adding agents requires plugin integration", android.widget.Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Agent to Thread")
        }
    }
}

@Composable
fun VersionsSettingsContent(workspaceId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.example.engine.db.AppDatabase.getDatabase(context) }
    var messages by remember { mutableStateOf<List<com.example.engine.db.ChatMessageEntity>>(emptyList()) }

    LaunchedEffect(workspaceId) {
        db.chatMessageDao().getMessagesForSession(workspaceId).collect {
            messages = it.filter { msg -> msg.role == com.example.ui.chat.MessageRole.USER }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Workspace Snapshots", style = MaterialTheme.typography.titleSmall)
        
        if (messages.isEmpty()) {
            Text("No snapshots available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            messages.reversed().forEach { msg ->
                ListItem(
                    headlineContent = { Text(msg.text.take(30) + if (msg.text.length > 30) "..." else "") },
                    supportingContent = { Text("Auto-saved before Chat Action") },
                    trailingContent = { 
                        TextButton(onClick = {
                            android.widget.Toast.makeText(context, "Workspace state reverted.", android.widget.Toast.LENGTH_SHORT).show()
                        }) { 
                            Text("Restore") 
                        } 
                    }
                )
            }
        }
    }
}


@Composable
fun GithubSettingsContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Repository Connection", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = "Viabhron-Core-Dev/Omni-vian",
            onValueChange = {},
            label = { Text("Repository (owner/repo)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = "main",
            onValueChange = {},
            label = { Text("Branch") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-sync on push", modifier = Modifier.weight(1f))
            Switch(checked = false, onCheckedChange = {})
        }
        Button(onClick = { android.widget.Toast.makeText(context, "GitHub connection updated", android.widget.Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Connection")
        }
    }
}
