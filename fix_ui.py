import re

path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerPanelScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Add necessary imports
imports_to_add = """
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Modifier
"""
# just put it near the top
content = content.replace("import androidx.compose.ui.platform.LocalContext", "import androidx.compose.ui.platform.LocalContext\n" + imports_to_add)

# Refactor AiManagerPanelScreen to define launcher and pass onImportClick
old_ai_manager_sig = """fun AiManagerPanelScreen(
    viewModel: AiManagerViewModel,
    onBack: () -> Unit
) {"""

new_ai_manager_sig = """fun AiManagerPanelScreen(
    viewModel: AiManagerViewModel,
    onBack: () -> Unit
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

content = content.replace(old_ai_manager_sig, new_ai_manager_sig)

# Change DirectoryTab call
content = content.replace("0 -> DirectoryTab(viewModel, onAddKeyClick)", "0 -> DirectoryTab(viewModel, onAddKeyClick, onImportClick)")

# Change DirectoryTab signature and implementation
old_dir_tab = """fun DirectoryTab(viewModel: AiManagerViewModel, onAddKeyClick: (String) -> Unit) {
    val providers by viewModel.providers.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(providers) { provider ->"""

new_dir_tab = """fun DirectoryTab(viewModel: AiManagerViewModel, onAddKeyClick: (String) -> Unit, onImportClick: () -> Unit) {
    val providers by viewModel.providers.collectAsState()
    val sortedProviders = providers.sortedByDescending { it.id == "local_gguf" }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(sortedProviders) { provider ->"""

content = content.replace(old_dir_tab, new_dir_tab)

old_dir_btn = """                        IconButton(onClick = { onAddKeyClick(provider.id) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Key")
                        }"""
new_dir_btn = """                        IconButton(onClick = { 
                            if (provider.id == "local_gguf") onImportClick() else onAddKeyClick(provider.id) 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = if (provider.id == "local_gguf") "Import GGUF" else "Add Key")
                        }"""
content = content.replace(old_dir_btn, new_dir_btn)

# Refactor ModelsTab
# Remove the old launcher from ModelsTab
pattern_launcher = re.compile(r"    val context = LocalContext\.current.*?Column\(modifier = Modifier\.fillMaxSize\(\)\) \{", re.DOTALL)
content = pattern_launcher.sub("    Column(modifier = Modifier.fillMaxSize()) {", content)

# Remove the buttons from ModelsTab
old_model_row = """        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Models", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
                    Text("Import .gguf")
                }
                Button(onClick = { viewModel.refreshModels() }) {
                    Text("Refresh")
                }
            }
        }"""
new_model_row = """        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Models", style = MaterialTheme.typography.titleMedium)
        }"""
content = content.replace(old_model_row, new_model_row)

# Add PullToRefreshBox around LazyColumn
old_lazy_col = """        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {"""

new_lazy_col = """        val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshModels() },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {"""
content = content.replace(old_lazy_col, new_lazy_col)

# Close the PullToRefreshBox
old_end_models = """            }
        }
    }
}

@Composable
fun ModelRaterTab"""

new_end_models = """            }
        }
        }
    }
}

@Composable
fun ModelRaterTab"""
content = content.replace(old_end_models, new_end_models)

with open(path, 'w') as f:
    f.write(content)
