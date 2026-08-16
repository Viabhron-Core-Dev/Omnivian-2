path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()
content = content.replace("import kotlinx.coroutines.flow.first\n", "import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.flow.asStateFlow\n")
with open(path, 'w') as f:
    f.write(content)
