sed -i '/var showContextMenu by remember { mutableStateOf(false) }/a \
    var showRenameDialog by remember { mutableStateOf(false) }\
    var showCopyDialog by remember { mutableStateOf(false) }\
    var showNewFileDialog by remember { mutableStateOf(false) }\
    var showNewFolderDialog by remember { mutableStateOf(false) }' app/src/main/java/com/example/ui/code/FileExplorer.kt

sed -i 's/onClick = { showContextMenu = false }/onClick = { showContextMenu = false; showRenameDialog = true }/' app/src/main/java/com/example/ui/code/FileExplorer.kt

sed -i 's/text = { Text("Copy") },/text = { Text("Copy") },/' app/src/main/java/com/example/ui/code/FileExplorer.kt
# Actually sed to replace specific ones is brittle. Let me just use python.
