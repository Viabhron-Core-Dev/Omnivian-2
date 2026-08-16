import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# Remove TokenUsageBar call
content = content.replace("        TokenUsageBar(usedTokens = 45000, maxTokens = 128000)\n", "")

# Add showTokenPanel state
content = content.replace(
    "    var showAgentSettings by remember { mutableStateOf(false) }",
    "    var showAgentSettings by remember { mutableStateOf(false) }\n    var showTokenPanel by remember { mutableStateOf(false) }"
)

# Add menu item for Token Panel
menu_item_replacement = """                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRename = true }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Token Panel") },
                        onClick = { showMenu = false; showTokenPanel = true }
                    )"""

content = content.replace("""                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRename = true }
                    )""", menu_item_replacement)

# Add bottom sheet trigger
bottom_sheet_replacement = """        if (showAgentSettings) {
            AgentSettingsBottomSheet(
                onDismiss = { showAgentSettings = false }
            )
        }
        
        if (showTokenPanel) {
            AiTokenPanelBottomSheet(
                onDismiss = { showTokenPanel = false }
            )
        }"""

content = content.replace("""        if (showAgentSettings) {
            AgentSettingsBottomSheet(
                onDismiss = { showAgentSettings = false }
            )
        }""", bottom_sheet_replacement)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
