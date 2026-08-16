import re
path = 'app/src/main/java/com/example/ui/chat/ChatScreen.kt'
with open(path, 'r') as f:
    content = f.read()

old_cancel = """                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            val index = chatMessages.indexOf(generatingMessage)
                                            if (index != -1) {
                                                val msg = generatingMessage.copy(text = "Generation stopped.")
                                                chatMessages[index] = msg
                                                saveMessage(msg)
                                            }
                                        } finally {"""
new_cancel = """                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                            if (index != -1) {
                                                val oldText = chatMessages[index].text
                                                val msg = chatMessages[index].copy(text = if (oldText.isBlank() || oldText.contains("Waking up")) "Generation stopped." else oldText)
                                                chatMessages[index] = msg
                                                saveMessage(msg)
                                            }
                                        } finally {"""

if old_cancel in content:
    content = content.replace(old_cancel, new_cancel)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced cancellation successfully")
else:
    print("Cancellation block not found")
