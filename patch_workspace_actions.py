import re

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'r') as f:
    content = f.read()

# Update signature
content = content.replace(
    "fun WorkspaceActionsBottomSheet(onDismiss: () -> Unit, onExportClick: () -> Unit = {}, onZipExportClick: () -> Unit = {}, onThreadSettingsClick: () -> Unit = {}) {",
    "fun WorkspaceActionsBottomSheet(onDismiss: () -> Unit, onExportClick: () -> Unit = {}, onZipExportClick: () -> Unit = {}, onThreadSettingsClick: () -> Unit = {}, onTokenPanelClick: () -> Unit = {}) {"
)

# Remove showOmniRouteDialog state
content = content.replace(
    "    var showOmniRouteDialog by remember { mutableStateOf(false) }\n",
    ""
)

# Update onClick
content = content.replace(
    "onClick = { showOmniRouteDialog = true }",
    "onClick = { onTokenPanelClick(); onDismiss() }"
)

# Remove AlertDialog
dialog_block = """    if (showOmniRouteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showOmniRouteDialog = false },
            title = { Text("AI Token Panel") },
            text = { Text("This will open the AI Manager Token Panel to view and manage your AI API usages, fallback chains, and active keys.") },
            confirmButton = {
                TextButton(onClick = { showOmniRouteDialog = false; onDismiss() }) {
                    Text("OK")
                }
            }
        )
    }
"""
content = content.replace(dialog_block, "")

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'w') as f:
    f.write(content)
