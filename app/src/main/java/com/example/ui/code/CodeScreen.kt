package com.example.ui.code

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import com.example.engine.server.PreviewServerManager
import com.example.engine.fs.FileNode
import com.example.engine.fs.FileHistoryEngine
import java.io.File
import com.example.engine.db.AppDatabase
import com.example.engine.db.WorkspacePullRequestEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeScreen(onMenuClick: () -> Unit) {
    var isFileExplorerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isServerRunning by PreviewServerManager.isRunning.collectAsState()
    var selectedFile by remember { mutableStateOf<FileNode?>(null) }
    val editorState = rememberCodeEditorState()
    
    var showMenu by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showSyntaxCheckDialog by remember { mutableStateOf(false) }
    var showSaveAsPRDialog by remember { mutableStateOf(false) }
    var showForkArtifactDialog by remember { mutableStateOf(false) }
    val workspaceName = remember(com.example.engine.fs.LocalFileManager.getWorkspaceDir().name) { 
        mutableStateOf(com.example.engine.fs.LocalFileManager.getWorkspaceName(com.example.engine.fs.LocalFileManager.getWorkspaceDir().name)) 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { 
                    Column {
                                Text(selectedFile?.name ?: workspaceName.value, style = MaterialTheme.typography.titleMedium)
                                if (editorState.isLiveGeneration) {
                                    Text("Live Generation View", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, "Global Menu")
                            }
                        },
                        actions = {
                            if (selectedFile != null) {
                                IconButton(onClick = { 
                                    editorState.saveFile()
                                    Toast.makeText(context, "Saved ${selectedFile?.name}", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Code", editorState.content.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = {
                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val fileToDownload = selectedFile?.file
                                    if (fileToDownload != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                                        try {
                                            fileToDownload.copyTo(java.io.File(downloadsDir, fileToDownload.name), overwrite = true)
                                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download")
                                }
                            }
                            
                            IconButton(onClick = {
                                if (isServerRunning) {
                                    PreviewServerManager.stop()
                                } else {
                                    PreviewServerManager.start(File(context.cacheDir, "workspace"))
                                }
                            }) {
                                Icon(
                                    if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isServerRunning) "Stop Server" else "Start Server"
                                )
                            }
                            IconButton(onClick = { isFileExplorerOpen = true }) {
                                Icon(Icons.Default.Folder, "File Tree")
                            }
                            
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "More Options")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    val currentWsDir = remember { com.example.engine.fs.LocalFileManager.getWorkspaceDir().name }
                                    val isArtifactWs = remember(currentWsDir) {
                                        currentWsDir.startsWith("artifact_") || com.example.engine.fs.ArtifactWorkspaceManager.getArtifactIdForWorkspace(currentWsDir) != null
                                    }

                                    if (isArtifactWs) {
                                        DropdownMenuItem(
                                            text = { Text("Save to Artifact") },
                                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showMenu = false
                                                editorState.saveFile()
                                                scope.launch {
                                                    val result = com.example.engine.fs.ArtifactWorkspaceManager.saveWorkspaceToArtifact(context, currentWsDir)
                                                    if (result.isSuccess) {
                                                        Toast.makeText(context, "Saved changes back to Artifact!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Fork / Save as New Artifact") },
                                            leadingIcon = { Icon(Icons.Default.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                            onClick = {
                                                showMenu = false
                                                editorState.saveFile()
                                                showForkArtifactDialog = true
                                            }
                                        )
                                        HorizontalDivider()
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Save as Artifact (mini app)") },
                                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showMenu = false
                                                editorState.saveFile()
                                                scope.launch {
                                                    val result = com.example.engine.fs.ArtifactWorkspaceManager.saveCurrentChatAsArtifact(context, currentWsDir, workspaceName.value)
                                                    if (result.isSuccess) {
                                                        Toast.makeText(context, "Saved to Artifacts (mini apps)!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Save failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        )
                                        HorizontalDivider()
                                    }

                                    DropdownMenuItem(
                                        text = { Text("Toggle Line Wrap: ${if (editorState.isLineWrapEnabled) "ON" else "OFF"}") },
                                        onClick = { 
                                            editorState.isLineWrapEnabled = !editorState.isLineWrapEnabled
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Find & Replace") },
                                        onClick = { 
                                            showFindReplaceDialog = true
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Go to Line") },
                                        onClick = { 
                                            showGoToLineDialog = true
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Check Syntax Error") },
                                        onClick = { 
                                            showSyntaxCheckDialog = true
                                            showMenu = false
                                        }
                                    )

                                }
                            }
                        }
                    )
                    
                    if (showFindReplaceDialog) {
                        FindReplaceDialog(editorState = editorState, onDismiss = { showFindReplaceDialog = false })
                    }
                    if (showGoToLineDialog) {
                        GoToLineDialog(editorState = editorState, onDismiss = { showGoToLineDialog = false })
                    }
                    if (showSyntaxCheckDialog) {
                        SyntaxCheckDialog(editorState = editorState, onDismiss = { showSyntaxCheckDialog = false })
                    }
                    
                    if (showForkArtifactDialog) {
                        var forkTitle by remember { mutableStateOf("${workspaceName.value} (Fork)") }
                        val currentWs = com.example.engine.fs.LocalFileManager.getWorkspaceDir().name
                        AlertDialog(
                            onDismissRequest = { showForkArtifactDialog = false },
                            title = { Text("Fork to New Artifact") },
                            text = {
                                OutlinedTextField(
                                    value = forkTitle,
                                    onValueChange = { forkTitle = it },
                                    label = { Text("Forked Artifact Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (forkTitle.isNotBlank()) {
                                        scope.launch {
                                            val result = com.example.engine.fs.ArtifactWorkspaceManager.forkWorkspaceToNewArtifact(context, currentWs, forkTitle.trim())
                                            if (result.isSuccess) {
                                                val (newEntity, _) = result.getOrThrow()
                                                workspaceName.value = newEntity.title
                                                Toast.makeText(context, "Forked to new artifact '${newEntity.title}'!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Fork failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                            showForkArtifactDialog = false
                                        }
                                    }
                                }) {
                                    Text("Fork")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showForkArtifactDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showSaveAsPRDialog && selectedFile != null) {
                        var prTitle by remember { mutableStateOf("") }
                        var prDesc by remember { mutableStateOf("") }
                        val db = remember { AppDatabase.getDatabase(context) }
                        
                        AlertDialog(
                            onDismissRequest = { showSaveAsPRDialog = false },
                            title = { Text("Save as Pull Request") },
                            text = {
                                Column {
                                    Text("Propose your changes to ${selectedFile?.name}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prTitle,
                                        onValueChange = { prTitle = it },
                                        label = { Text("PR Title") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prDesc,
                                        onValueChange = { prDesc = it },
                                        label = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        val pr = WorkspacePullRequestEntity(
                                            id = UUID.randomUUID().toString(),
                                            workspaceId = com.example.engine.fs.LocalFileManager.getWorkspaceDir().name,
                                            title = prTitle,
                                            description = prDesc,
                                            targetFile = selectedFile!!.file.absolutePath,
                                            diff = editorState.content.text, // Simply storing current editor state as diff for now
                                            status = "open",
                                            createdAt = System.currentTimeMillis()
                                        )
                                        db.workspacePullRequestDao().savePullRequest(pr)
                                        showSaveAsPRDialog = false
                                        Toast.makeText(context, "Pull Request Created", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("Submit PR")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveAsPRDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }



                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        selectedFile?.let { fileNode ->
                            val name = fileNode.name.lowercase()
                            when {
                                name.endsWith(".pdf") -> PdfViewer(fileNode.file)
                                name.endsWith(".ppt") || name.endsWith(".pptx") -> PptViewer(fileNode.file)
                                name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") -> {
                                    Text("Image Viewer not implemented yet", modifier = Modifier.padding(16.dp))
                                }
                                else -> TextViewer(fileNode.file, editorState)
                            }
                        } ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a file to view", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
        
        AnimatedVisibility(
            visible = isFileExplorerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        ) {
            Surface(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                FileExplorer(
                    onFileClick = { fileNode ->
                        selectedFile = fileNode
                        isFileExplorerOpen = false
                    },
                    onCloseClick = { isFileExplorerOpen = false }
                )
            }
        }
    }
}

@Composable
fun FileRevertDialog(file: File, onDismiss: () -> Unit, onRevert: (File) -> Unit) {
    val revisions = remember(file) { FileHistoryEngine.getRevisions(file) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File History & Revert") },
        text = {
            if (revisions.isEmpty()) {
                Text("No history available for this file.")
            } else {
                LazyColumn {
                    items(revisions) { revision ->
                        ListItem(
                            headlineContent = { Text(revision.name) },
                            supportingContent = { Text("Size: ${revision.length()} bytes") },
                            trailingContent = {
                                TextButton(onClick = { onRevert(revision) }) {
                                    Text("Revert")
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
