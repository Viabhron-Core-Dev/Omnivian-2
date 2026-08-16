import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'r') as f:
    content = f.read()

replacement = """fun ModelsTab(viewModel: AiManagerViewModel) {
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
            Button(onClick = { viewModel.refreshModels() }) {
                Text("Refresh")
            }
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

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
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
}"""

content = re.sub(r'fun ModelsTab\(viewModel: AiManagerViewModel\) \{.*', replacement, content, flags=re.DOTALL)

# Also rename OmniRoute to OmniRoot
content = content.replace('OmniRoute AI Manager', 'OmniRoot AI Manager')

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'w') as f:
    f.write(content)
