import re
path = 'app/src/main/java/com/example/ui/chat/ChatScreen.kt'
with open(path, 'r') as f:
    content = f.read()

imports = """import com.example.engine.omniroot.local.LlamaEngine
import kotlinx.coroutines.flow.first
"""
if "import com.example.engine.omniroot.local.LlamaEngine" not in content:
    content = content.replace("import com.example.engine.db.AppDatabase", imports + "import com.example.engine.db.AppDatabase")

old_generate_block = """                                    currentJob = scope.launch {
                                        try {
                                            val response = com.example.ui.chat.OmniRootClient.generateContent(
                                                chatMessages.filter { it.id != generatingMessage.id },
                                                selectedModel
                                            )
                                            val index = chatMessages.indexOf(generatingMessage)
                                            if (index != -1) {
                                                chatMessages.removeAt(index)
                                            }
                                            
                                            if (response.actions.isNotEmpty() || response.editedFiles.isNotEmpty()) {
                                                val msg = ChatMessage(text = "", role = MessageRole.APP_ACTION, appActions = response.actions, editedFiles = response.editedFiles)
                                                chatMessages.add(msg)
                                                saveMessage(msg)
                                            }

                                            if (!response.text.isNullOrBlank()) {
                                                val msg = ChatMessage(text = response.text, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)

                                                chatMessages.add(msg)
                                                saveMessage(msg)
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {"""

new_generate_block = """                                    currentJob = scope.launch {
                                        try {
                                            if (currentProvider == "local_gguf") {
                                                // Mini-Phase 3 & 4: Direct Bypass and Streaming UI
                                                val models = db.aiModelDao().getAllModels().first()
                                                val modelEntity = models.firstOrNull { it.providerId == "local_gguf" && it.modelId == currentModel }
                                                val absolutePath = modelEntity?.description ?: currentModel
                                                
                                                val llama = LlamaEngine(context)
                                                val loaded = llama.loadModelSafely(absolutePath)
                                                
                                                if (loaded) {
                                                    var combinedPrompt = ""
                                                    chatMessages.filter { it.id != generatingMessage.id }.forEach { msg ->
                                                        val roleStr = if (msg.role == MessageRole.USER) "user" else "assistant"
                                                        combinedPrompt += "$roleStr\\n${msg.text}\\n"
                                                    }
                                                    
                                                    var streamedText = ""
                                                    llama.predictFlow(combinedPrompt).collect { token ->
                                                        streamedText += token
                                                        val index = chatMessages.indexOf(generatingMessage)
                                                        if (index != -1) {
                                                            chatMessages[index] = generatingMessage.copy(text = streamedText)
                                                        }
                                                    }
                                                    llama.unloadModel()
                                                    
                                                    // Final save
                                                    val index = chatMessages.indexOf(generatingMessage)
                                                    if (index != -1) {
                                                        val finalMsg = chatMessages[index]
                                                        saveMessage(finalMsg)
                                                    }
                                                } else {
                                                    val index = chatMessages.indexOf(generatingMessage)
                                                    if (index != -1) {
                                                        val finalMsg = generatingMessage.copy(text = "Error: Local model failed to load (OOM or File Not Found).")
                                                        chatMessages[index] = finalMsg
                                                        saveMessage(finalMsg)
                                                    }
                                                }
                                                
                                            } else {
                                                // Normal OmniRoot HTTP Proxy flow
                                                val response = com.example.ui.chat.OmniRootClient.generateContent(
                                                    chatMessages.filter { it.id != generatingMessage.id },
                                                    selectedModel
                                                )
                                                val index = chatMessages.indexOf(generatingMessage)
                                                if (index != -1) {
                                                    chatMessages.removeAt(index)
                                                }
                                                
                                                if (response.actions.isNotEmpty() || response.editedFiles.isNotEmpty()) {
                                                    val msg = ChatMessage(text = "", role = MessageRole.APP_ACTION, appActions = response.actions, editedFiles = response.editedFiles)
                                                    chatMessages.add(msg)
                                                    saveMessage(msg)
                                                }

                                                if (!response.text.isNullOrBlank()) {
                                                    val msg = ChatMessage(text = response.text, role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)

                                                    chatMessages.add(msg)
                                                    saveMessage(msg)
                                                }
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {"""

if old_generate_block in content:
    content = content.replace(old_generate_block, new_generate_block)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Generate block not found")
