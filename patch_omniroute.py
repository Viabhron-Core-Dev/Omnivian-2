import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

# Add state
content = content.replace(
    "    var showGithubExport by remember { mutableStateOf(false) }",
    "    var showGithubExport by remember { mutableStateOf(false) }\n    var showTokenPanel by remember { mutableStateOf(false) }"
)

# Update WorkspaceActionsBottomSheet parameters
content = content.replace(
    "                        onThreadSettingsClick = {\n                            showWorkspaceActions = false\n                            navController.navigate(\"thread_settings\")\n                        }",
    "                        onThreadSettingsClick = {\n                            showWorkspaceActions = false\n                            navController.navigate(\"thread_settings\")\n                        },\n                        onTokenPanelClick = {\n                            showTokenPanel = true\n                        }"
)

# Render AiTokenPanelBottomSheet
render_panel = """                if (showGithubExport) {
                    GithubExportBottomSheet(
                        onDismiss = { showGithubExport = false }
                    )
                }
                
                if (showTokenPanel) {
                    com.example.ui.chat.AiTokenPanelBottomSheet(
                        onDismiss = { showTokenPanel = false }
                    )
                }"""
content = content.replace(
    """                if (showGithubExport) {
                    GithubExportBottomSheet(
                        onDismiss = { showGithubExport = false }
                    )
                }""",
    render_panel
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
