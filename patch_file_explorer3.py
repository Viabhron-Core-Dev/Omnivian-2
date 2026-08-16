import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Add state vars for dialogs
old_vars = """    var showNewFolderDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current"""

new_vars = """    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showCreateIssueDialog by remember { mutableStateOf(false) }
    var showCreatePRDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current"""

content = content.replace(old_vars, new_vars)

old_dropdown = """                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showContextMenu = false
                            scope.launch { LocalFileManager.deleteFile(node.file) }
                        }
                    )"""

new_dropdown = """                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showContextMenu = false
                            scope.launch { LocalFileManager.deleteFile(node.file) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Issue") },
                        onClick = { showContextMenu = false; showCreateIssueDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Create PR") },
                        onClick = { showContextMenu = false; showCreatePRDialog = true }
                    )"""

content = content.replace(old_dropdown, new_dropdown)

# Add Dialogs at the end of the composable (after the Row and Column closures, need to find the right spot)
# `FileTreeNodeView` ends around line 350. Let's find `    } // End of Column` or something similar.
# Wait, the column contains `Row` then `if (isExpanded) { node.children.forEach ... }` then dialogs.

old_end_dialog = """        if (showNewFolderDialog) {
            var folderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val newFolder = java.io.File(node.file, folderName)
                            if (newFolder.mkdirs()) {
                                LocalFileManager.refreshFileTree()
                            }
                            showNewFolderDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}"""

new_end_dialog = """        if (showNewFolderDialog) {
            var folderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val newFolder = java.io.File(node.file, folderName)
                            if (newFolder.mkdirs()) {
                                LocalFileManager.refreshFileTree()
                            }
                            showNewFolderDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        val db = remember { AppDatabase.getDatabase(context) }
        val workspaceId = LocalFileManager.getWorkspaceDir().name
        
        if (showCreateIssueDialog) {
            var issueTitle by remember { mutableStateOf("") }
            var issueDesc by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateIssueDialog = false },
                title = { Text("Create Issue for ${node.name}") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = issueTitle,
                            onValueChange = { issueTitle = it },
                            label = { Text("Issue Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = issueDesc,
                            onValueChange = { issueDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val issue = WorkspaceIssueEntity(
                                id = UUID.randomUUID().toString(),
                                workspaceId = workspaceId,
                                title = issueTitle,
                                description = issueDesc,
                                targetFile = node.file.absolutePath,
                                status = "open",
                                createdAt = System.currentTimeMillis()
                            )
                            db.workspaceIssueDao().saveIssue(issue)
                            showCreateIssueDialog = false
                        }
                    }) {
                        Text("Create Issue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateIssueDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showCreatePRDialog) {
            var prTitle by remember { mutableStateOf("") }
            var prDesc by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePRDialog = false },
                title = { Text("Create PR for ${node.name}") },
                text = {
                    Column {
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
                                workspaceId = workspaceId,
                                title = prTitle,
                                description = prDesc,
                                targetFile = node.file.absolutePath,
                                diff = "Proposed change for ${node.name} (Diff not available for direct PR creation)",
                                status = "open",
                                createdAt = System.currentTimeMillis()
                            )
                            db.workspacePullRequestDao().savePullRequest(pr)
                            showCreatePRDialog = false
                        }
                    }) {
                        Text("Create PR")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePRDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}"""

content = content.replace(old_end_dialog, new_end_dialog)

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)
