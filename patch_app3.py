with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                            AppTab.CHAT -> key(chatSessionId) {
                                LaunchedEffect(chatSessionId) {
                                    com.example.engine.fs.LocalFileManager.switchWorkspace(chatSessionId)
                                }
                                ChatScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }""",
"""                            AppTab.CHAT -> key(chatSessionId) {
                                remember(chatSessionId) {
                                    com.example.engine.fs.LocalFileManager.switchWorkspace(chatSessionId)
                                    true
                                }
                                ChatScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }"""
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

