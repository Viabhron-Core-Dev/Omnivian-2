import re

with open('app/src/main/java/com/example/engine/db/ChatMessageMapper.kt', 'r') as f:
    content = f.read()

replacement = """
    return ChatMessageEntity(
        id = this.id,
        sessionId = sessionId,
        text = this.text,
        role = this.role,
        modelName = this.modelName,
        providerId = this.providerId,
        editedFilesJson = editedFilesJson.toString(),
        appActionsJson = appActionsJson.toString(),
        timestamp = System.currentTimeMillis()
    )
"""
content = re.sub(r'    return ChatMessageEntity\(.*?    \)', replacement, content, flags=re.DOTALL)

replacement_2 = """
    return ChatMessage(
        id = this.id,
        text = this.text,
        role = this.role,
        modelName = this.modelName,
        providerId = this.providerId,
        editedFiles = editedFiles,
        appActions = appActions
    )
"""
content = re.sub(r'    return ChatMessage\(.*?    \)', replacement_2, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/engine/db/ChatMessageMapper.kt', 'w') as f:
    f.write(content)
