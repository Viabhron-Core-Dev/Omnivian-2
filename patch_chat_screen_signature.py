import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('fun ChatScreen(\n    onMenuClick: () -> Unit\n)', 'fun ChatScreen(\n    sessionId: String,\n    onMenuClick: () -> Unit\n)')

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
