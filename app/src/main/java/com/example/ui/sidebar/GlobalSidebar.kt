package com.example.ui.sidebar

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
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlobalSidebar(
    onClose: () -> Unit, 
    onNavigateToSettings: () -> Unit = {}, 
    onNavigateToArtifacts: () -> Unit = {},
    onNewChat: () -> Unit = {},
    currentChatId: String,
    onChatSelected: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
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
            label = { Text("Artifacts (mini apps)") },
            selected = false,
            onClick = { 
                onNavigateToArtifacts()
                onClose()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.DesignServices, contentDescription = null) },
            label = { Text("Design") },
            selected = false,
            onClick = { 
                android.widget.Toast.makeText(context, "Design Studio coming soon", android.widget.Toast.LENGTH_SHORT).show()
                onClose()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
            label = { Text("Library") },
            selected = false,
            onClick = { 
                android.widget.Toast.makeText(context, "Component Library coming soon", android.widget.Toast.LENGTH_SHORT).show()
                onClose()
            },
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
                        val workspaceToDelete = workspace.name
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val db = com.example.engine.db.AppDatabase.getDatabase(context)
                            db.chatMessageDao().clearSession(workspaceToDelete)
                            db.workspaceConfigDao().deleteConfig(workspaceToDelete)
                        }
                        LocalFileManager.deleteWorkspace(workspace.name)
                        val remaining = LocalFileManager.getWorkspaces()
                        workspaces = remaining
                        if (workspace.name == currentChatId) {
                            if (remaining.isNotEmpty()) {
                                onChatSelected(remaining.first().name)
                            } else {
                                onNewChat()
                            }
                        }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var chatName by remember { mutableStateOf(LocalFileManager.getWorkspaceName(workspace.name)) }
    val timestamp = remember(workspace.lastModified()) {
        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(workspace.lastModified()))
    }

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
        label = { 
            Column {
                Text(chatName, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
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
                        onClick = { 
                            showMenu = false
                            android.widget.Toast.makeText(context, "Archive requires Google Drive integration", android.widget.Toast.LENGTH_SHORT).show()
                        }
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
                    LocalFileManager.setWorkspaceName(workspace.name, newName)
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
