import re

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("msg.role == com.example.ui.chat.MessageRole.USER.name", "msg.role == com.example.ui.chat.MessageRole.USER")

with open('app/src/main/java/com/example/ui/settings/ThreadSettingsScreen.kt', 'w') as f:
    f.write(content)
