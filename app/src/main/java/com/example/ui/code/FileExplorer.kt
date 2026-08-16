package com.example.ui.code

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.engine.fs.FileNode
import com.example.engine.fs.LocalFileManager
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import com.example.engine.db.AppDatabase
import com.example.engine.db.WorkspaceIssueEntity
import com.example.engine.db.WorkspacePullRequestEntity
import java.util.UUID


@Composable
fun FileExplorer(
    onFileClick: (FileNode) -> Unit,
    onCloseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fileTree by LocalFileManager.fileTreeState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val workspaceName = remember(LocalFileManager.getWorkspaceDir().name) { 
        mutableStateOf(LocalFileManager.getWorkspaceName(LocalFileManager.getWorkspaceDir().name)) 
    }

    val workspaceId = LocalFileManager.getWorkspaceDir().name
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Files", "Issues", "PRs")
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close Drawer")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = workspaceName.value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search files...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        if (selectedTabIndex == 0) {
            fileTree?.let { rootNode ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isNotEmpty()) {
                    val searchResults = searchFileTree(rootNode, searchQuery)
                    if (searchResults.isEmpty()) {
                        item {
                            Text("No files found", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(searchResults) { node ->
                            SearchResultItem(node = node, onFileClick = onFileClick)
                        }
                    }
                } else {
                    item {
                        FileTreeNodeView(
                            node = rootNode,
                            level = 0,
                            onFileClick = onFileClick
                        )
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workspace found")
            }
        }
        } else if (selectedTabIndex == 1) {
            IssuesTab(workspaceId = workspaceId)
        } else if (selectedTabIndex == 2) {
            PullRequestsTab(workspaceId = workspaceId)
        }
    }
}

@Composable
fun IssuesTab(workspaceId: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val issues by db.workspaceIssueDao().getIssuesForWorkspace(workspaceId).collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (issues.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No local issues found.")
                }
            }
        }
        items(issues) { issue ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = issue.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: ${issue.status}", style = MaterialTheme.typography.bodySmall)
                    if (issue.targetFile != null) {
                        Text(text = "Target: ${issue.targetFile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PullRequestsTab(workspaceId: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prs by db.workspacePullRequestDao().getPullRequestsForWorkspace(workspaceId).collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (prs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No local pull requests found.")
                }
            }
        }
        items(prs) { pr ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = pr.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: ${pr.status}", style = MaterialTheme.typography.bodySmall)
                    if (pr.targetFile != null) {
                        Text(text = "Target: ${pr.targetFile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

fun searchFileTree(node: FileNode, query: String): List<FileNode> {
    val results = mutableListOf<FileNode>()
    if (node.name.contains(query, ignoreCase = true)) {
        results.add(node)
    }
    if (node.isDirectory) {
        node.children.forEach { child ->
            results.addAll(searchFileTree(child, query))
        }
    }
    return results
}

@Composable
fun SearchResultItem(
    node: FileNode,
    onFileClick: (FileNode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!node.isDirectory) {
                    onFileClick(node)
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FileTreeNodeView(
    node: FileNode,
    level: Int,
    onFileClick: (FileNode) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showCreateIssueDialog by remember { mutableStateOf(false) }
    var showCreatePRDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val uploadFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    // Get original file name if possible
                    var fileName = "uploaded_file_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                    }
                    val targetFile = java.io.File(node.file, fileName)
                    LocalFileManager.copyUriToFile(context, uri, targetFile)
                }
            }
        }
    }
    
    val uploadZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                // Copy zip to a temporary file
                val tempZip = java.io.File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.zip")
                val result = LocalFileManager.copyUriToFile(context, uri, tempZip)
                if (result.isSuccess) {
                    LocalFileManager.unzipFile(tempZip, node.file)
                    tempZip.delete()
                    LocalFileManager.refreshFileTree()
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        isExpanded = !isExpanded
                    } else {
                        onFileClick(node)
                    }
                }
                .padding(start = (level * 16 + 16).dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFileIcon(node, isExpanded),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showContextMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(16.dp))
            }
            
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { showContextMenu = false; showRenameDialog = true }
                )
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = { showContextMenu = false; showCopyDialog = true }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showContextMenu = false
                        scope.launch { LocalFileManager.deleteFile(node.file) }
                    }
                )
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("New File") },
                        onClick = { showContextMenu = false; showNewFileDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("New Folder") },
                        onClick = { showContextMenu = false; showNewFolderDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress to Zip") },
                        onClick = { 
                            showContextMenu = false
                            scope.launch {
                                val targetZip = java.io.File(node.file.parentFile, "${node.file.name}.zip")
                                LocalFileManager.zipDirectory(node.file, targetZip)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Upload Files") },
                        onClick = { 
                            showContextMenu = false
                            uploadFilesLauncher.launch("*/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Upload and Extract Zip") },
                        onClick = { 
                            showContextMenu = false
                            uploadZipLauncher.launch("application/zip")
                        }
                    )
                } else if (node.name.endsWith(".zip")) {
                     DropdownMenuItem(
                        text = { Text("Extract Here") },
                        onClick = { 
                            showContextMenu = false
                            scope.launch {
                                val targetDir = java.io.File(node.file.parentFile, node.file.nameWithoutExtension)
                                LocalFileManager.unzipFile(node.file, targetDir)
                            }
                        }
                    )
                }
            }
        }


        if (showRenameDialog) {
            var newName by remember { mutableStateOf(node.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { LocalFileManager.renameFile(node.file, newName) }
                        showRenameDialog = false
                    }) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
            )
        }
        if (showCopyDialog) {
            var newName by remember { mutableStateOf(node.name + "_copy") }
            AlertDialog(
                onDismissRequest = { showCopyDialog = false },
                title = { Text("Copy") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { 
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                node.file.copyTo(java.io.File(node.file.parentFile, newName), overwrite = true)
                            }
                            LocalFileManager.refreshFileTree()
                        }
                        showCopyDialog = false
                    }) { Text("Copy") }
                },
                dismissButton = { TextButton(onClick = { showCopyDialog = false }) { Text("Cancel") } }
            )
        }
        if (showNewFileDialog) {
            var newName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewFileDialog = false },
                title = { Text("New File") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch { LocalFileManager.createFile(node.file, newName, false) }
                        }
                        showNewFileDialog = false
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") } }
            )
        }
        if (showNewFolderDialog) {
            var newName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("New Folder") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch { LocalFileManager.createFile(node.file, newName, true) }
                        }
                        showNewFolderDialog = false
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") } }
            )
        }

        if (isExpanded && node.isDirectory) {
            node.children.forEach { child ->
                FileTreeNodeView(
                    node = child,
                    level = level + 1,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

private fun getFileIcon(node: FileNode, isExpanded: Boolean): ImageVector {
    return when {
        node.isDirectory && isExpanded -> Icons.Default.FolderOpen
        node.isDirectory -> Icons.Default.Folder
        node.name.endsWith(".kt") || node.name.endsWith(".java") -> Icons.Default.Code
        node.name.endsWith(".json") || node.name.endsWith(".xml") -> Icons.Default.DataObject
        node.name.endsWith(".md") || node.name.endsWith(".txt") -> Icons.Default.Description
        node.name.endsWith(".pdf") -> Icons.Default.PictureAsPdf
        node.name.endsWith(".zip") -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }
}
