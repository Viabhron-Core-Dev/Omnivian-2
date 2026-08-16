import re
path = 'app/src/main/java/com/example/ui/chat/ChatScreen.kt'
with open(path, 'r') as f:
    content = f.read()

old_think = """                                    val generatingMessage = ChatMessage(text = "Thinking...", role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)"""
new_think = """                                    val loadingText = if (currentProvider == "local_gguf") "Waking up $currentModel in RAM..." else "Thinking..."
                                    val generatingMessage = ChatMessage(text = loadingText, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)"""

if old_think in content:
    content = content.replace(old_think, new_think)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Thinking logic not found")
