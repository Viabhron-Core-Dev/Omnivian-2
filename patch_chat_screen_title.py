with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""fun ChatScreen(
    onMenuClick: () -> Unit
) {""",
"""fun ChatScreen(
    onMenuClick: () -> Unit
) {
    val workspaceName = remember { mutableStateOf(com.example.engine.fs.LocalFileManager.getWorkspaceName(com.example.engine.fs.LocalFileManager.getWorkspaceDir().name)) }"""
)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)

