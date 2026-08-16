import re

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.unit.dp", "import androidx.compose.ui.unit.dp\nimport kotlinx.coroutines.launch")

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'w') as f:
    f.write(content)
