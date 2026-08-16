import re

# Update ViewModel
path_vm = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path_vm, 'r') as f:
    vm_content = f.read()

import_statement = "import android.content.Context\nimport kotlinx.coroutines.flow.MutableStateFlow\n"
if "import android.content.Context" not in vm_content:
    vm_content = vm_content.replace("import android.app.Application", import_statement + "import android.app.Application")

old_add = """    fun addLocalModel(fileName: String, uriString: String) {
        viewModelScope.launch {
            val (iType, oType) = inferModelTypes(fileName)
            aiModelDao.insertModels(
                listOf(
                    com.example.engine.db.AiModelEntity(
                        providerId = "local_gguf",
                        modelId = fileName,
                        
                        description = uriString,
                        inputType = iType,
                        outputType = oType
                    )
                )
            )
            refreshModels()
        }
    }"""

new_add = """    val isImporting = MutableStateFlow(false)
    val importProgress = MutableStateFlow(0f)

    fun addLocalModel(context: Context, fileName: String, uri: android.net.Uri) {
        viewModelScope.launch {
            isImporting.value = true
            importProgress.value = 0f
            try {
                val modelsDir = java.io.File(context.filesDir, "models")
                if (!modelsDir.exists()) modelsDir.mkdirs()
                val targetFile = java.io.File(modelsDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        var size = -1L
                        cursor?.use { c ->
                            if (c.moveToFirst()) {
                                val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                if (sizeIndex != -1) size = c.getLong(sizeIndex)
                            }
                        }
                        
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (size > 0) {
                                importProgress.value = (totalRead.toFloat() / size.toFloat()).coerceIn(0f, 1f)
                            } else {
                                importProgress.value = (importProgress.value + 0.01f) % 1f
                            }
                        }
                    }
                }

                val (iType, oType) = inferModelTypes(fileName)
                aiModelDao.insertModels(
                    listOf(
                        com.example.engine.db.AiModelEntity(
                            providerId = "local_gguf",
                            modelId = fileName,
                            description = targetFile.absolutePath,
                            inputType = iType,
                            outputType = oType
                        )
                    )
                )
                refreshModels()
            } catch (e: Exception) {
                Log.e("AiManagerViewModel", "Error importing model", e)
            } finally {
                isImporting.value = false
            }
        }
    }"""

vm_content = vm_content.replace(old_add, new_add)
with open(path_vm, 'w') as f:
    f.write(vm_content)


# Update UI (Panel Screen)
path_ui = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerPanelScreen.kt'
with open(path_ui, 'r') as f:
    ui_content = f.read()

if "import androidx.compose.ui.window.Dialog" not in ui_content:
    ui_content = ui_content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport androidx.compose.ui.window.Dialog")
if "import androidx.compose.runtime.collectAsState" not in ui_content:
    ui_content = ui_content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.collectAsState")

old_call = """            viewModel.addLocalModel(fileName, it.toString())"""
new_call = """            viewModel.addLocalModel(context, fileName, it)"""
ui_content = ui_content.replace(old_call, new_call)

dialog_code = """
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    if (isImporting) {
        Dialog(onDismissRequest = { }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = { importProgress }, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Copying GGUF to secure internal storage...")
                    Text("${(importProgress * 100).toInt()}%")
                }
            }
        }
    }
"""

if "val isImporting by" not in ui_content:
    # Insert it right before "Scaffold("
    ui_content = ui_content.replace("    Scaffold(", dialog_code + "\n    Scaffold(")

with open(path_ui, 'w') as f:
    f.write(ui_content)
