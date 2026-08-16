package com.example.ui.settings.omniroot

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

import android.provider.OpenableColumns
import android.net.Uri
import android.database.Cursor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiManagerPanelScreen(
    onNavigateBack: () -> Unit,
    onAddKeyClick: (String) -> Unit,
    viewModel: AiManagerViewModel = viewModel()
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            var fileName = "local_model.gguf"
            val cursor: Cursor? = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) fileName = c.getString(displayNameIndex)
                }
            }
            viewModel.addLocalModel(context, fileName, it)
        }
    }
    
    val onImportClick: () -> Unit = { launcher.launch(arrayOf("*/*")) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Directory", "Active Keys", "Available Models", "Metrics", "Model Rater", "Translator")


    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    if (isImporting) {
        Dialog(onDismissRequest = { }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = { importProgress }, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Copying GGUF to secure internal storage...")
                    Text("${(importProgress * 100).toInt()}%")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OmniRoot AI Manager") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTabIndex) {
                    0 -> DirectoryTab(viewModel, onAddKeyClick, onImportClick)
                    1 -> ActiveKeysTab(viewModel)
                    2 -> ModelsTab(viewModel)
                    3 -> MetricsTab(viewModel)
                    4 -> ModelRaterTab(viewModel)
                    5 -> TranslatorTab()
                }
            }
        }
    }
}

@Composable
fun CenterTextTab(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text)
    }
}

@Composable
fun DirectoryTab(viewModel: AiManagerViewModel, onAddKeyClick: (String) -> Unit, onImportClick: () -> Unit) {
    val providers by viewModel.providers.collectAsState()
    val sortedProviders = providers.sortedByDescending { it.id == "local_gguf" }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(sortedProviders) { provider ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                ListItem(
                    headlineContent = { Text(provider.name) },
                    supportingContent = { Text(provider.description) },
                    leadingContent = { Icon(Icons.Default.Business, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { 
                            if (provider.id == "local_gguf") onImportClick() else onAddKeyClick(provider.id) 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = if (provider.id == "local_gguf") "Import GGUF" else "Add Key")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveKeysTab(viewModel: AiManagerViewModel) {
    val keys by viewModel.activeKeys.collectAsState()
    val providers by viewModel.providers.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        if (keys.isEmpty()) {
            item { CenterTextTab("No active keys configured.") }
        } else {
            items(keys) { key ->
                val provider = providers.find { it.id == key.providerId }?.name ?: key.providerId
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    ListItem(
                        headlineContent = { Text("${key.alias} ($provider)") },
                        supportingContent = { Text(key.keyMasked) },
                        leadingContent = { Icon(Icons.Default.Key, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsTab(viewModel: AiManagerViewModel) {
    val tokens by viewModel.totalTokens.collectAsState()
    val requests by viewModel.totalRequests.collectAsState()
    val cost by viewModel.totalCost.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ListItem(
                headlineContent = { Text("Total Tokens Used") },
                trailingContent = { Text((tokens ?: 0).toString()) }
            )
        }
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ListItem(
                headlineContent = { Text("Total Requests") },
                trailingContent = { Text(requests.toString()) }
            )
        }
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ListItem(
                headlineContent = { Text("Estimated Cost") },
                trailingContent = { Text(String.format("$%.4f", cost ?: 0.0)) }
            )
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ModelsTab(viewModel: AiManagerViewModel) {
    val modelEntities by viewModel.availableModelEntities.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var sortBy by remember { mutableStateOf("Name") } // "Name" or "Type"
    
    // Group models by provider
    val groupedModels = remember(modelEntities, searchQuery, sortBy) {
        val map = mutableMapOf<String, MutableList<com.example.engine.db.AiModelEntity>>()
        
        modelEntities.forEach { entity ->
            val provider = entity.providerId
            val modelName = entity.modelId
            
            if (searchQuery.isBlank() || modelName.contains(searchQuery, ignoreCase = true) || provider.contains(searchQuery, ignoreCase = true)) {
                if (!map.containsKey(provider)) {
                    map[provider] = mutableListOf()
                }
                map[provider]?.add(entity)
            }
        }
        
        // Sort within folders
        map.forEach { (_, list) ->
            if (sortBy == "Type") {
                list.sortBy { it.outputType + it.modelId }
            } else {
                list.sortBy { it.modelId }
            }
        }
        
        map
    }


    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Models", style = MaterialTheme.typography.titleMedium)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    // Auto-expand all if searching
                    if (it.isNotBlank()) {
                        expandedFolders = groupedModels.keys.toSet()
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            // Sort Dropdown
            var sortMenuExpanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = true,
                    onClick = { sortMenuExpanded = true },
                    label = { Text("Sort: $sortBy") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortBy = "Name"; sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Sort by Type") }, onClick = { sortBy = "Type"; sortMenuExpanded = false })
                }
            }
        }

        val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshModels() },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (modelEntities.isEmpty()) {
                item {
                    CenterTextTab("No models fetched (Add keys and Refresh)")
                }
            } else {
                groupedModels.forEach { (provider, modelList) ->
                    item(key = "header_$provider") {
                        val isExpanded = expandedFolders.contains(provider)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                expandedFolders = if (isExpanded) {
                                    expandedFolders - provider
                                } else {
                                    expandedFolders + provider
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, 
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(provider.uppercase(), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("${modelList.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    item {
                        Column {
                            AnimatedVisibility(visible = expandedFolders.contains(provider) || searchQuery.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 4.dp, bottom = 8.dp)) {
                                modelList.forEach { entity ->
                                    val icon = when (entity.outputType) {
                                        "AUDIO" -> Icons.Default.Mic
                                        "IMAGE" -> Icons.Default.Image
                                        "EMBEDDING" -> Icons.Default.Storage
                                        "UNSUPPORTED" -> Icons.Default.Warning
                                        else -> Icons.Default.ChatBubbleOutline
                                    }
                                    val tint = if (entity.outputType == "TEXT") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    val textColor = if (entity.outputType == "TEXT") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = entity.outputType, modifier = Modifier.size(16.dp), tint = tint)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(entity.modelId, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                            if (entity.outputType != "TEXT") {
                                                Text(entity.outputType, style = MaterialTheme.typography.labelSmall, color = tint)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun ModelRaterTab(viewModel: AiManagerViewModel) {
    val ratings by viewModel.modelRatings.collectAsState()
    
    if (ratings.isEmpty()) {
        CenterTextTab("No ratings yet. Rate messages in Chat!")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ratings.sortedByDescending { it.upvotes - it.downvotes }) { stat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stat.modelName) },
                        supportingContent = { Text(stat.providerId) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Upvotes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stat.upvotes.toString())
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(Icons.Default.ThumbDown, contentDescription = "Downvotes", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stat.downvotes.toString())
                            }
                        }
                    )
                }
            }
        }
    }
}
