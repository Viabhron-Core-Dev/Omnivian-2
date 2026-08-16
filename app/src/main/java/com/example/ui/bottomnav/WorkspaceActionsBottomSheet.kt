package com.example.ui.bottomnav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceActionsBottomSheet(onDismiss: () -> Unit, onExportClick: () -> Unit = {}, onZipExportClick: () -> Unit = {}, onThreadSettingsClick: () -> Unit = {}, onTokenPanelClick: () -> Unit = {}) {
    var showExportOptions by remember { mutableStateOf(false) }
    var showRemixDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionGridItem(
                    icon = Icons.Default.Settings,
                    label = "Thread Settings",
                    onClick = { showExportOptions = false; onThreadSettingsClick() },
                    modifier = Modifier.size(80.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Share,
                    label = "Export",
                    onClick = { showExportOptions = true },
                    modifier = Modifier.size(80.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionGridItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = "Remix",
                    onClick = { showRemixDialog = true },
                    modifier = Modifier.size(80.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Dashboard,
                    label = "AI Token Panel",
                    onClick = { onTokenPanelClick(); onDismiss() },
                    modifier = Modifier.size(80.dp)
                )
            }
        }

    if (showRemixDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemixDialog = false },
            title = { Text("Remix Workspace") },
            text = { Text("This will copy the repository and all integrations (agents, skills, settings) into a new workspace, clearing chat history. (Feature coming soon)") },
            confirmButton = {
                TextButton(onClick = { showRemixDialog = false; onDismiss() }) {
                    Text("OK")
                }
            }
        )
    }


    if (showExportOptions) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExportOptions = false },
            title = { Text("Export") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Export as ZIP") },
                        leadingContent = { Icon(Icons.Default.FolderZip, null) },
                        modifier = Modifier.clickable { 
                            showExportOptions = false 
                            onDismiss() 
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Push to GitHub") },
                        leadingContent = { Icon(Icons.Default.Code, null) },
                        modifier = Modifier.clickable { 
                            showExportOptions = false 
                            onExportClick()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    }
}

@Composable
private fun ActionGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
