import re

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

replacement_1 = """
                                    val parts = selectedModel.split("/", limit = 2)
                                    val currentProvider = parts.getOrNull(0)
                                    val currentModel = parts.getOrNull(1) ?: selectedModel

                                    val generatingMessage = ChatMessage(text = "Thinking...", role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)
"""
content = content.replace('                                    val generatingMessage = ChatMessage(text = "Thinking...", role = MessageRole.AI)', replacement_1)

replacement_2 = """
                                            if (!response.text.isNullOrBlank()) {
                                                val msg = ChatMessage(text = response.text, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)
"""
content = content.replace("""                                            if (!response.text.isNullOrBlank()) {
                                                val msg = ChatMessage(text = response.text, role = MessageRole.AI)""", replacement_2)


with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)
