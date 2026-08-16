import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Add missing vars inside FileExplorer composable
content = content.replace(
    "    Column(modifier = modifier.fillMaxSize()) {",
    "    val workspaceId = LocalFileManager.getWorkspaceDir().name\n    var selectedTabIndex by remember { mutableIntStateOf(0) }\n    val tabs = listOf(\"Files\", \"Issues\", \"PRs\")\n    Column(modifier = modifier.fillMaxSize()) {"
)

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)
