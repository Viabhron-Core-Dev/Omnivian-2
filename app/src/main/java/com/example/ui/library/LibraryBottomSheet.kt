package com.example.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Component Library (GDrive)", style = MaterialTheme.typography.titleLarge)
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder") }
                    IconButton(onClick = {}) { Icon(Icons.Default.UploadFile, contentDescription = "Upload") }
                }
            }
            
            Text("All files uploaded in app are managed here and synced to GDrive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ListItem(
                        headlineContent = { Text("Images") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Documents") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("mockup_v1.png") },
                        supportingContent = { Text("1.2 MB • Just now") },
                        leadingContent = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                        modifier = Modifier.clickable { }
                    )
                }
            }
        }
    }
}
