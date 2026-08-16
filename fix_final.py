import re

# Fix ViewModel
path_vm = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path_vm, 'r') as f:
    content_vm = f.read()
content_vm = content_vm.replace("kotlinx.coroutines.flow.asStateFlow(_isRefreshing)", "_isRefreshing.asStateFlow()")
with open(path_vm, 'w') as f:
    f.write(content_vm)

# Fix PanelScreen
path_ui = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerPanelScreen.kt'
with open(path_ui, 'r') as f:
    content_ui = f.read()

# Remove duplicate imports of Modifier
content_ui = content_ui.replace("import androidx.compose.ui.Modifier\n", "")
# Re-add just one instance
content_ui = content_ui.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.ui.Modifier")

# Add OptIn for ModelsTab
content_ui = content_ui.replace("fun ModelsTab(viewModel: AiManagerViewModel)", "@OptIn(ExperimentalMaterial3Api::class)\nfun ModelsTab(viewModel: AiManagerViewModel)")

with open(path_ui, 'w') as f:
    f.write(content_ui)
