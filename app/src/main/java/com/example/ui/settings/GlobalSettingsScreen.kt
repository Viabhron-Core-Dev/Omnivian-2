package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsCategory("Core Setup")
                SettingsItem("OmniRoot", "Local proxy and model settings", Icons.Default.Router) { onNavigateTo("settings/omniroot") }
                SettingsItem("Agents", "Manage built agents", Icons.Default.SmartToy) { onNavigateTo("settings/agents") }
                SettingsItem("Skills", "Manage shared skills", Icons.Default.Psychology) { onNavigateTo("settings/skills") }
                SettingsItem("Tools", "Manage tool permissions", Icons.Default.Build) { onNavigateTo("settings/tools") }
                SettingsItem("MCP", "Manage Context Providers", Icons.Default.AccountTree) { onNavigateTo("settings/mcp") }
                SettingsItem("Plugins", "User-made plugins", Icons.Default.Extension) { onNavigateTo("settings/plugins") }
                SettingsItem("Memory Modules", "Manage agent memory types and architectures", Icons.Default.Memory) { onNavigateTo("settings/memory_modules") }
                SettingsItem("Audio & Speech", "Speech recognition, imported models, and TTS voice", Icons.Default.GraphicEq) { onNavigateTo("settings/audio") }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsCategory("Integrations")
                SettingsItem("GitHub Settings", "Requires connection", Icons.Default.Code, enabled = false) { onNavigateTo("settings/github") }
                SettingsItem("Firebase Settings", "Requires connection", Icons.Default.CloudSync, enabled = false) { onNavigateTo("settings/firebase") }
                SettingsItem("GDrive Settings", "Requires connection", Icons.Default.AddToDrive, enabled = false) { onNavigateTo("settings/gdrive") }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsCategory("App & IDE")
                SettingsItem("Code Editor", "Editor preferences", Icons.Default.Edit) { onNavigateTo("settings/editor") }
                SettingsItem("Artifacts (Preview)", "Artifact generation settings", Icons.Default.Preview) { onNavigateTo("settings/artifacts") }
                SettingsItem("Library Management", "GDrive library settings", Icons.Default.FolderSpecial) { onNavigateTo("settings/library") }
                SettingsItem("Permissions", "System permissions used", Icons.Default.Security) { onNavigateTo("settings/permissions") }
                SettingsItem("Font & Typography", "App-wide font settings", Icons.Default.FontDownload) { onNavigateTo("settings/font") }
                SettingsItem("Encrypted Backup", "Backup and restore your data securely", Icons.Default.Lock) { onNavigateTo("settings/backup") }
                SettingsItem("Log Keeper", "View and export app logs", Icons.Default.BugReport) { onNavigateTo("log_keeper") }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(
            headlineColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            leadingIconColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    )
}
