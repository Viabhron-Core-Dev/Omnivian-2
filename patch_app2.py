with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""            GlobalSidebar(
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = { chatSessionId = java.util.UUID.randomUUID().toString() },
                onNavigateToSettings = { 
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                }
            )""",
"""            GlobalSidebar(
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = { chatSessionId = java.util.UUID.randomUUID().toString() },
                onNavigateToSettings = { 
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                },
                currentChatId = chatSessionId,
                onChatSelected = { newSessionId -> chatSessionId = newSessionId }
            )"""
)

# And also need to update LocalFileManager on switch
content = content.replace(
"""                            AppTab.CHAT -> key(chatSessionId) {
                                ChatScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }""",
"""                            AppTab.CHAT -> key(chatSessionId) {
                                LaunchedEffect(chatSessionId) {
                                    com.example.engine.fs.LocalFileManager.switchWorkspace(chatSessionId)
                                }
                                ChatScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }"""
)


with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

