import re
path = 'app/src/main/java/com/example/ui/chat/ChatScreen.kt'
with open(path, 'r') as f:
    content = f.read()

imports = """import com.example.engine.omniroot.local.LocalAiManager
import com.example.utils.LogKeeper
import kotlinx.coroutines.flow.onCompletion
"""
if "import com.example.engine.omniroot.local.LocalAiManager" not in content:
    content = content.replace("import com.example.engine.omniroot.local.LlamaEngine", imports + "import com.example.engine.omniroot.local.LlamaEngine")

old_bypass = """                                                val llama = LlamaEngine(context)
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
                                                }"""

new_bypass = """                                                val llama = LocalAiManager.getOrLoadEngine(context, absolutePath)
                                                
                                                if (llama != null) {
                                                    var combinedPrompt = ""
                                                    chatMessages.filter { it.id != generatingMessage.id }.forEach { msg ->
                                                        val roleStr = if (msg.role == MessageRole.USER) "user" else "assistant"
                                                        combinedPrompt += "<|im_start|>$roleStr\\n${msg.text}<|im_end|>\\n"
                                                    }
                                                    combinedPrompt += "<|im_start|>assistant\\n"
                                                    
                                                    var streamedText = ""
                                                    val startTime = System.currentTimeMillis()
                                                    var tokenCount = 0
                                                    
                                                    llama.predictFlow(combinedPrompt).collect { token ->
                                                        streamedText += token
                                                        tokenCount++
                                                        
                                                        val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                        if (index != -1) {
                                                            chatMessages[index] = generatingMessage.copy(text = streamedText)
                                                        }
                                                    }
                                                    
                                                    val endTime = System.currentTimeMillis()
                                                    val elapsedSec = (endTime - startTime) / 1000.0
                                                    val tps = if (elapsedSec > 0) tokenCount / elapsedSec else 0.0
                                                    LogKeeper.log("Local AI", "Metrics", "Stream finished. Tokens: $tokenCount, Time: ${elapsedSec}s, TPS: $tps")
                                                    
                                                    // Note: We DO NOT unloadModel() here anymore. We keep it alive in LocalAiManager!
                                                    
                                                    // Final save
                                                    val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                    if (index != -1) {
                                                        val finalMsg = chatMessages[index]
                                                        saveMessage(finalMsg)
                                                    }
                                                } else {
                                                    val index = chatMessages.indexOfFirst { it.id == generatingMessage.id }
                                                    if (index != -1) {
                                                        val finalMsg = generatingMessage.copy(text = "Error: Local model failed to load (OOM or File Not Found).")
                                                        chatMessages[index] = finalMsg
                                                        saveMessage(finalMsg)
                                                    }
                                                }"""

if old_bypass in content:
    content = content.replace(old_bypass, new_bypass)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Bypass block not found")
