import re

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'r') as f:
    content = f.read()

# Replace GlobalSidebar
new_content = """package com.example.ui.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.fs.LocalFileManager
import java.io.File
import androidx.compose.ui.Alignment

@Composable
fun GlobalSidebar(
    onClose: () -> Unit, 
    onNavigateToSettings: () -> Unit = {}, 
    onNewChat: () -> Unit = {},
    currentChatId: String,
    onChatSelected: (String) -> Unit = {}
) {
    var workspaces by remember { mutableStateOf(emptyList<File>()) }
    
    LaunchedEffect(currentChatId, onClose) { // Trigger reload when sidebar opens or chat changes
        workspaces = LocalFileManager.getWorkspaces()
    }

    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        HorizontalDivider()
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            label = { Text("New Chat") },
            selected = false,
            onClick = { onNewChat(); onClose() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Code, contentDescription = null) },
            label = { Text("Artifacts") },
            selected = false,
            onClick = { /* TODO: Artifacts */ onClose() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.DesignServices, contentDescription = null) },
            label = { Text("Design") },
            selected = false,
            onClick = { /* TODO: Design Studio */ onClose() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
            label = { Text("Library") },
            selected = false,
            onClick = { /* TODO: Library */ onClose() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text("List of Chats (Repos)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(workspaces) { workspace ->
                ChatSidebarItem(
                    workspace = workspace,
                    isSelected = workspace.name == currentChatId,
                    onClick = { 
                        onChatSelected(workspace.name)
                        onClose()
                    },
                    onDelete = {
                        LocalFileManager.deleteWorkspace(workspace.name)
                        workspaces = LocalFileManager.getWorkspaces()
                    }
                )
            }
        }
        
        HorizontalDivider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Global Settings") },
            selected = false,
            onClick = onNavigateToSettings,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ChatSidebarItem(
    workspace: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var chatName by remember { mutableStateOf(workspace.name) } // TODO: Read from metadata in the future

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
        label = { Text(chatName) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
        badge = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRename = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive (GDrive)") },
                        onClick = { showMenu = false; /* TODO */ }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    )

    if (showRename) {
        var newName by remember { mutableStateOf(chatName) }
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
                    chatName = newName
                    // In a real implementation we would save this name to a metadata file in the workspace
                    showRename = false 
                }) {
                    Text("Rename")
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
"""

with open('app/src/main/java/com/example/ui/sidebar/GlobalSidebar.kt', 'w') as f:
    f.write(new_content)

