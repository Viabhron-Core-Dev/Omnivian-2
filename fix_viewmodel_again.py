import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()

replacement = """                    suspend fun applyFallbacks() {
                        val fallbacks = when(provider.id) {
                            "anthropic" -> listOf("claude-3-5-sonnet-20240620", "claude-3-opus-20240229")
                            "google_ai_studio" -> listOf("gemini-1.5-pro-latest", "gemini-1.5-flash-latest")
                            "openai" -> listOf("gpt-4o", "gpt-4o-mini", "text-embedding-3-large", "tts-1")
                            "openrouter" -> listOf("meta-llama/llama-3-8b-instruct:free")
                            "groq" -> listOf("llama3-8b-8192", "whisper-large-v3")
                            "together_ai" -> listOf("meta-llama/Llama-3-8b-chat-hf")
                            "local_gguf" -> listOf("local-model")
                            else -> emptyList()
                        }
                        if (fallbacks.isNotEmpty()) {
                            aiModelDao.deleteModelsForProvider(provider.id)
                            aiModelDao.insertModels(fallbacks.map { 
                                val (iType, oType) = inferModelTypes(it)
                                AiModelEntity(providerId = provider.id, modelId = it, inputType = iType, outputType = oType) 
                            })
                        }
                    }
                    
                    try {
                        val reqBuilder = Request.Builder().url(modelsUrl)
                        if (provider.id == "anthropic") {
                            reqBuilder.addHeader("x-api-key", key.keyValue)
                            reqBuilder.addHeader("anthropic-version", "2023-06-01")
                        } else {
                            reqBuilder.addHeader("Authorization", "Bearer " + key.keyValue)
                        }
                        
                        val response = httpClient.newCall(reqBuilder.build()).execute()
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            val modelsResp = try { modelsAdapter.fromJson(responseBody) } catch(e: Exception) { null }
                            val entities = modelsResp?.data?.map { 
                                val (iType, oType) = inferModelTypes(it.id)
                                AiModelEntity(providerId = provider.id, modelId = it.id, inputType = iType, outputType = oType) 
                            } ?: emptyList()
                            
                            if (entities.isEmpty()) {
                                applyFallbacks()
                            } else {
                                aiModelDao.deleteModelsForProvider(provider.id)
                                aiModelDao.insertModels(entities)
                            }
                        } else {
                            Log.e("AiManager", "Failed to fetch models for ${provider.id}: ${response.code} $responseBody")
                            applyFallbacks()
                        }
                    } catch (e: Exception) {
                        Log.e("AiManager", "Exception fetching models for ${provider.id}", e)
                        applyFallbacks()
                    }"""

content = re.sub(r'                    suspend fun applyFallbacks\(\) \{.*?applyFallbacks\(\)\n                    \}', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'w') as f:
    f.write(content)
