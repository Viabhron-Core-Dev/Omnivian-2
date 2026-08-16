import re

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'r') as f:
    content = f.read()

# Change size of items
content = content.replace("Modifier.size(80.dp)", "Modifier.size(110.dp)")

# Change padding
content = content.replace("padding(bottom = 16.dp, top = 16.dp)", "padding(bottom = 32.dp, top = 16.dp)")
content = content.replace("Arrangement.spacedBy(8.dp)", "Arrangement.spacedBy(16.dp)")
content = content.replace("padding(4.dp)", "padding(8.dp)")

# Rename OmniRoute Dashboard
content = content.replace('"AI Manager Token Panel"', '"OmniRoute Dashboard"')
content = content.replace("This will open the AI Manager Token Panel to view and manage your AI API usages, fallback chains, and active keys.", "This will open the integrated proxy web interface for local AI models and routing. (Feature pending OmniRoute proxy integration)")

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'w') as f:
    f.write(content)
