import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

content = content.replace("chatSessionId = newSessionId", "chatSessionId = newSessionId\n                    val context = navController.context")

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)
