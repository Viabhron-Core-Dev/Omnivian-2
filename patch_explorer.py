import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Add states
content = content.replace(
    'var showContextMenu by remember { mutableStateOf(false) }',
    '''var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }'''
)

# Replace DropdownMenuItem actions
content = content.replace(
    'text = { Text("Rename") },\n                    onClick = { showContextMenu = false }',
    'text = { Text("Rename") },\n                    onClick = { showContextMenu = false; showRenameDialog = true }'
)
content = content.replace(
    'text = { Text("Copy") },\n                    onClick = { showContextMenu = false }',
    'text = { Text("Copy") },\n                    onClick = { showContextMenu = false; showCopyDialog = true }'
)
content = content.replace(
    'text = { Text("New File") },\n                        onClick = { showContextMenu = false }',
    'text = { Text("New File") },\n                        onClick = { showContextMenu = false; showNewFileDialog = true }'
)
content = content.replace(
    'text = { Text("New Folder") },\n                        onClick = { showContextMenu = false }',
    'text = { Text("New Folder") },\n                        onClick = { showContextMenu = false; showNewFolderDialog = true }'
)

# Add Dialogs at the end of FileTreeNodeView
dialogs = """
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
                            kotlinx.coroutines.Dispatchers.IO.invoke {
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
"""
content = content.replace(
    '        if (isExpanded && node.isDirectory) {',
    dialogs + '\n        if (isExpanded && node.isDirectory) {'
)

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)

