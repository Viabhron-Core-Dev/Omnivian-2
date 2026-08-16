with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                            AppTab.CHAT -> ChatScreen(
                                key = chatSessionId,
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )""",
"""                            AppTab.CHAT -> key(chatSessionId) {
                                ChatScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }"""
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

