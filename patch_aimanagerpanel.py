import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'r') as f:
    content = f.read()

# Update AiManagerPanelScreen signature
content = content.replace(
    "fun AiManagerPanelScreen(\n    onNavigateBack: () -> Unit,\n    viewModel: AiManagerViewModel = viewModel()\n)",
    "fun AiManagerPanelScreen(\n    onNavigateBack: () -> Unit,\n    onAddKeyClick: (String) -> Unit,\n    viewModel: AiManagerViewModel = viewModel()\n)"
)

# Pass onAddKeyClick to DirectoryTab
content = content.replace(
    "DirectoryTab(viewModel)",
    "DirectoryTab(viewModel, onAddKeyClick)"
)

# Update DirectoryTab signature and IconButton
content = content.replace(
    "fun DirectoryTab(viewModel: AiManagerViewModel)",
    "fun DirectoryTab(viewModel: AiManagerViewModel, onAddKeyClick: (String) -> Unit)"
)
content = content.replace(
    "IconButton(onClick = { viewModel.addMockKey(provider.id) }) {",
    "IconButton(onClick = { onAddKeyClick(provider.id) }) {"
)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'w') as f:
    f.write(content)
