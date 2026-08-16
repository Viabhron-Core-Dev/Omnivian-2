import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

# find the mutableStateListOf block and replace it
new_content = re.sub(
    r'mutableStateListOf\(\s*ChatMessage\(.*?\)\s*\)\s*\}',
    'mutableStateListOf<ChatMessage>()\n    }',
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(new_content)
