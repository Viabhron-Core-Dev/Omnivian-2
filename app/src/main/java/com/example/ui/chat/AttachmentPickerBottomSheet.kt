package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerBottomSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (AttachmentOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showGithubDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onOptionSelected(AttachmentOption.ImageUri(it)) }
        if (uri != null) onDismiss()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onOptionSelected(AttachmentOption.FileUri(it)) }
        if (uri != null) onDismiss()
    }

    if (showGithubDialog) {
        var repoUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGithubDialog = false },
            title = { Text("Import from GitHub") },
            text = {
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGithubDialog = false
                        onOptionSelected(AttachmentOption.GithubRepo(repoUrl))
                        onDismiss()
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGithubDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Add Attachment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            AttachmentOptionItem(
                icon = Icons.Default.CameraAlt,
                title = "Camera",
                subtitle = "Take a photo directly",
                onClick = {
                    onOptionSelected(AttachmentOption.LaunchCamera)
                    onDismiss()
                }
            )

            AttachmentOptionItem(
                icon = Icons.Default.Image,
                title = "Images",
                subtitle = "Select an image from device",
                onClick = { imagePickerLauncher.launch("image/*") }
            )
            
            AttachmentOptionItem(
                icon = Icons.Default.InsertDriveFile,
                title = "Files",
                subtitle = "Select a document or file",
                onClick = { filePickerLauncher.launch("*/*") }
            )
            
            AttachmentOptionItem(
                icon = Icons.Default.AutoAwesome,
                title = "Knowledge Bits",
                subtitle = "Insert cached code reference, GitHub file, or table",
                onClick = {
                    onOptionSelected(AttachmentOption.KnowledgeBits)
                    onDismiss()
                }
            )

            AttachmentOptionItem(
                icon = Icons.Default.Code,
                title = "GitHub Repository",
                subtitle = "Import code from a GitHub link",
                onClick = { showGithubDialog = true }
            )
            
            AttachmentOptionItem(
                icon = Icons.Default.Cloud,
                title = "Google Drive",
                subtitle = "Select from Google Drive (Coming Soon)",
                onClick = { 
                    onOptionSelected(AttachmentOption.GoogleDrive)
                    onDismiss()
                }
            )
            
            AttachmentOptionItem(
                icon = Icons.Default.Folder,
                title = "Workspace Artifacts",
                subtitle = "Select a file from current workspace",
                onClick = { 
                    onOptionSelected(AttachmentOption.Workspace)
                    onDismiss()
                }
            )

            AttachmentOptionItem(
                icon = Icons.Default.GraphicEq,
                title = "Audio & Speech Settings",
                subtitle = "Configure STT engine, offline models, and voice",
                onClick = { 
                    onOptionSelected(AttachmentOption.AudioSettings)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

sealed class AttachmentOption {
    object LaunchCamera : AttachmentOption()
    data class ImageUri(val uri: Uri) : AttachmentOption()
    data class FileUri(val uri: Uri) : AttachmentOption()
    object KnowledgeBits : AttachmentOption()
    data class GithubRepo(val url: String) : AttachmentOption()
    object GoogleDrive : AttachmentOption()
    object Workspace : AttachmentOption()
    object AudioSettings : AttachmentOption()
}
