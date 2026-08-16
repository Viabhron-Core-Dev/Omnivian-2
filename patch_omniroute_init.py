import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

replacement = """    var chatSessionId by remember { 
        mutableStateOf(java.util.UUID.randomUUID().toString().also { newId ->
            val count = com.example.engine.fs.LocalFileManager.getWorkspaces().size
            com.example.engine.fs.LocalFileManager.setWorkspaceName(newId, "Chat ${count + 1}")
        }) 
    }"""

content = re.sub(r'    var chatSessionId by remember \{ mutableStateOf\(java\.util\.UUID\.randomUUID\(\)\.toString\(\)\) \}', replacement, content)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
