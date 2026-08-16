import re
path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace('                            "local_gguf" -> listOf("local-model")', '                            // local_gguf should not be hardcoded since we parse actual files\n                            "local_gguf" -> emptyList()')

with open(path, 'w') as f:
    f.write(content)

