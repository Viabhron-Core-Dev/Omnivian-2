import re

path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

local_inference_code = """
                    val updatedRequest = request.copy(model = actualModelName)
                    
                    if (providerId == "local_gguf") {
                        LogKeeper.log("Proxy", "Executing local inference via llama.cpp for model: $actualModelName", "")
                        
                        var combinedInputText = ""
                        updatedRequest.messages.forEach { combinedInputText += it.content + "\\n" }
                        
                        // We use the model name as the URI since we stored it in the DB (or could fetch it)
                        val llama = com.example.engine.omniroot.local.LlamaEngine(context)
                        val loaded = llama.loadModelSafely(actualModelName)
                        
                        if (loaded) {
                            val prediction = llama.predict(combinedInputText)
                            llama.unloadModel()
                            
                            val localResponse = OmniResponse(
                                id = "chatcmpl-local",
                                model = actualModelName,
                                choices = listOf(
                                    OmniChoice(
                                        index = 0,
                                        message = OmniMessage("assistant", prediction),
                                        finish_reason = "stop"
                                    )
                                )
                            )
                            val jsonLocalResponse = moshi.adapter(OmniResponse::class.java).toJson(localResponse)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", jsonLocalResponse)
                        } else {
                            lastErrorResponse = "Local model failed to load (OOM or File Not Found)"
                            lastCode = 500
                            continue
                        }
                    }
"""

content = content.replace('                    val updatedRequest = request.copy(model = actualModelName)', local_inference_code)

with open(path, 'w') as f:
    f.write(content)

print("Patched Proxy successfully.")
