import re

path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerPanelScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Fix imports
content = content.replace("import androidx.compose.ui.Modifier\nimport android.provider.OpenableColumns", "import android.provider.OpenableColumns")

# Define launcher properly inside AiManagerPanelScreen
old_sig = """fun AiManagerPanelScreen(
    onNavigateBack: () -> Unit,
    onAddKeyClick: (String) -> Unit,
    viewModel: AiManagerViewModel = viewModel()
) {"""

new_sig = """fun AiManagerPanelScreen(
    onNavigateBack: () -> Unit,
    onAddKeyClick: (String) -> Unit,
    viewModel: AiManagerViewModel = viewModel()
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            var fileName = "local_model.gguf"
            val cursor: Cursor? = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) fileName = c.getString(displayNameIndex)
                }
            }
            viewModel.addLocalModel(fileName, it.toString())
        }
    }
    
    val onImportClick: () -> Unit = { launcher.launch(arrayOf("*/*")) }
"""
content = content.replace(old_sig, new_sig)

# Fix AnimatedVisibility issue by removing it or importing it properly if it's the receiver error.
# The error was: 'fun ColumnScope.AnimatedVisibility... cannot be called in this context with an implicit receiver'
# It's inside a LazyColumn `item { AnimatedVisibility }` which is not a ColumnScope!
# We must wrap it in a Column inside `item { Column { AnimatedVisibility { ... } } }`
old_item = """                    item {
                        AnimatedVisibility(visible = expandedFolders.contains(provider) || searchQuery.isNotBlank()) {"""

new_item = """                    item {
                        Column {
                            AnimatedVisibility(visible = expandedFolders.contains(provider) || searchQuery.isNotBlank()) {"""

content = content.replace(old_item, new_item)

old_item_end = """                                }
                            }
                        }
                    }"""

new_item_end = """                                }
                            }
                        }
                        }
                    }"""

content = content.replace(old_item_end, new_item_end)

with open(path, 'w') as f:
    f.write(content)
