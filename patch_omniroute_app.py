import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'ChatScreen(\n                                    onMenuClick = { scope.launch { drawerState.open() } }\n                                )',
    'ChatScreen(\n                                    sessionId = chatSessionId,\n                                    onMenuClick = { scope.launch { drawerState.open() } }\n                                )'
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
