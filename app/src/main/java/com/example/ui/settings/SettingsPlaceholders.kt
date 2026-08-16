package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment

@Composable
fun SkillsSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Skills", style = MaterialTheme.typography.titleMedium)
        Text("Manage shared skills available to all agents.")
        Button(onClick = {}) { Text("Add Skill") }
    }
}

@Composable
fun ToolsSettingsContent() {
    var searchQuery by remember { mutableStateOf("") }
    
    // Tools Model
    data class Tool(val name: String, val description: String, var permission: String = "Always Ask")
    data class MCPCategory(val name: String, val tools: List<Tool>, var isExpanded: Boolean = false)
    
    val mcpCategories = remember {
        mutableStateListOf(
            MCPCategory(
                name = "On-Device (Local)",
                isExpanded = false,
                tools = listOf(
                    Tool("Edit File", "Modify local workspace files"),
                    Tool("View File", "Read local workspace files"),
                    Tool("Create PR", "Create a local Pull Request"),
                    Tool("Create Issue", "Create a local Issue"),
                    Tool("Run Command", "Execute local shell commands"),
                    Tool("JS Sandbox", "Execute lightweight JS scripts")
                )
            ),
            MCPCategory(
                name = "GitHub",
                isExpanded = false,
                tools = listOf(
                    Tool("Search Repos", "Search GitHub repositories"),
                    Tool("Get Issue", "Fetch issue details"),
                    Tool("Create Comment", "Comment on PRs/Issues")
                )
            ),
            MCPCategory(
                name = "Google Drive",
                isExpanded = false,
                tools = listOf(
                    Tool("Search Drive", "Search for files on Google Drive"),
                    Tool("Read Document", "Read Docs/Sheets content")
                )
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tools", style = MaterialTheme.typography.titleMedium)
        Text("Manage tool permissions. Tools list refreshes when MCPs are connected.", style = MaterialTheme.typography.bodyMedium)
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search tools...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            mcpCategories.forEachIndexed { catIndex, category ->
                val filteredTools = category.tools.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
                
                if (filteredTools.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    mcpCategories[catIndex] = category.copy(isExpanded = !category.isExpanded) 
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.name, style = MaterialTheme.typography.titleSmall)
                                Icon(
                                    if (category.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    
                    if (category.isExpanded || searchQuery.isNotEmpty()) {
                        items(filteredTools.size) { toolIndex ->
                            val tool = filteredTools[toolIndex]
                            var showDropdown by remember { mutableStateOf(false) }
                            
                            ListItem(
                                headlineContent = { Text(tool.name) },
                                supportingContent = { Text(tool.description) },
                                trailingContent = {
                                    Box {
                                        TextButton(onClick = { showDropdown = true }) {
                                            Text(tool.permission)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        DropdownMenu(
                                            expanded = showDropdown,
                                            onDismissRequest = { showDropdown = false }
                                        ) {
                                            val options = listOf("Always Ask", "Use Freely", "No Permission")
                                            options.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        // Update permission in mutable list
                                                        val updatedTools = category.tools.toMutableList()
                                                        updatedTools[toolIndex] = tool.copy(permission = option)
                                                        mcpCategories[catIndex] = category.copy(tools = updatedTools)
                                                        showDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    FloatingActionButton(
        onClick = { /* TODO */ },
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Create Tool")
    }
}
}

@Composable
fun MCPSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Context Providers (MCP)", style = MaterialTheme.typography.titleMedium)
        Text("Connect external data sources.")
        Button(onClick = {}) { Text("Connect GDrive MCP") }
        Button(onClick = {}) { Text("Connect GitHub MCP") }
    }
}

@Composable
fun PluginsSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Plugins", style = MaterialTheme.typography.titleMedium)
        Text("User-made combinations of skills, tools, and MCPs.")
        Button(onClick = {}) { Text("Create Plugin") }
    }
}

@Composable
fun IntegrationsSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Integrations", style = MaterialTheme.typography.titleMedium)
        Text("Manage third-party connections.")
    }
}

@Composable
fun PermissionsSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("System Permissions", style = MaterialTheme.typography.titleMedium)
        ListItem(headlineContent = { Text("Storage") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
        ListItem(headlineContent = { Text("Internet") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
    }
}

@Composable
fun FontSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Font & Typography", style = MaterialTheme.typography.titleMedium)
        Text("Select app-wide font.")
        ListItem(headlineContent = { Text("Inter") }, trailingContent = { RadioButton(selected = true, onClick = {}) })
        ListItem(headlineContent = { Text("Roboto") }, trailingContent = { RadioButton(selected = false, onClick = {}) })
    }
}

@Composable
fun BackupSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Encrypted Backup & Restore", style = MaterialTheme.typography.titleMedium)
        Text("Securely backup your workspace, settings, and local keys to an encrypted file.")
        Button(onClick = {}) { Text("Create Encrypted Backup") }
        Button(onClick = {}) { Text("Restore from Backup") }
    }
}

@Composable
fun EditorSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Code Editor Settings", style = MaterialTheme.typography.titleMedium)
        Text("Manage editor preferences.")
        ListItem(headlineContent = { Text("Word Wrap") }, trailingContent = { Switch(checked = false, onCheckedChange = {}) })
        ListItem(headlineContent = { Text("Show Line Numbers") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
        ListItem(headlineContent = { Text("Auto-Indent") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
    }
}

@Composable
fun MemoryModulesSettingsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Memory Modules (Phase 11)", style = MaterialTheme.typography.titleMedium)
        Text("Manage agent memory architectures (Episodic, Vector/RAG, File-System).")
        Button(onClick = {}) { Text("Add Memory Module") }
    }
}
