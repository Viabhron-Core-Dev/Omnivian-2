import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Add imports
content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.compose.ui.platform.LocalContext\nimport android.provider.OpenableColumns")

old_dialog_vars = """    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }"""

new_dialog_vars = """    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    
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
    }"""

content = content.replace(old_dialog_vars, new_dialog_vars)

old_dropdown = """                    DropdownMenuItem(
                        text = { Text("Compress to Zip") },
                        onClick = { 
                            showContextMenu = false
                            scope.launch {
                                val targetZip = java.io.File(node.file.parentFile, "${node.file.name}.zip")
                                LocalFileManager.zipDirectory(node.file, targetZip)
                            }
                        }
                    )"""

new_dropdown = """                    DropdownMenuItem(
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
                    )"""

content = content.replace(old_dropdown, new_dropdown)

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)
