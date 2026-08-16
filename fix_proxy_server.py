import re
path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

# I want to intercept tools in OmniRootProxyServer after translation.
old_logic = """                        val standardResponse = TranslationEngine.translateResponse(responseBody, targetFormat)
                        
                        // Calculate tokens and cost"""

new_logic = """                        var standardResponse = TranslationEngine.translateResponse(responseBody, targetFormat)
                        
                        // Phase 9.5 Interception: Intercept read_file / write_file / list_files
                        val toolCalls = standardResponse.choices?.firstOrNull()?.message?.tool_calls
                        if (!toolCalls.isNullOrEmpty()) {
                            for (tc in toolCalls) {
                                val funcName = tc.function.name
                                if (funcName == "read_file" || funcName == "write_file" || funcName == "list_files") {
                                    val result = com.example.engine.omniroot.tools.NativeToolExecutor.execute(context, funcName, tc.function.arguments)
                                    // Normally we would append the tool result and call the LLM again.
                                    // For now, we will just return the tool execution result as an assistant message 
                                    // to make it visible, or we could leave the tool_call in the response for the client.
                                    // Let's modify the response to include the tool result as content.
                                    standardResponse = standardResponse.copy(
                                        choices = listOf(
                                            com.example.ui.chat.OmniChoice(
                                                message = com.example.ui.chat.OmniMessage(
                                                    role = "assistant",
                                                    content = "Tool executed: $funcName\\nResult: $result"
                                                )
                                            )
                                        )
                                    )
                                    break
                                }
                            }
                        }
                        
                        // Calculate tokens and cost"""

if old_logic in content:
    content = content.replace(old_logic, new_logic)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Logic not found")
