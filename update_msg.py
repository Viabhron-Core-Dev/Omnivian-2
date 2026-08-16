import re

# Update ChatMessageEntity
with open('app/src/main/java/com/example/engine/db/ChatMessageEntity.kt', 'r') as f:
    content = f.read()

replacement = """
    val role: MessageRole,
    val modelName: String? = null,
    val providerId: String? = null,
    val editedFilesJson: String,
"""
content = re.sub(r'\n    val role: MessageRole,\n    val editedFilesJson: String,', replacement, content)

with open('app/src/main/java/com/example/engine/db/ChatMessageEntity.kt', 'w') as f:
    f.write(content)

# Update ChatMessage
with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

replacement_2 = """
    val role: MessageRole = MessageRole.USER,
    val modelName: String? = null,
    val providerId: String? = null,
    val editedFiles: List<Pair<String, Boolean>> = emptyList(),
"""
content = re.sub(r'\n    val role: MessageRole = MessageRole\.USER,\n    val editedFiles: List<Pair<String, Boolean>> = emptyList\(\),', replacement_2, content)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
