import re

with open('app/src/main/java/com/example/ui/chat/GeminiClient.kt', 'r') as f:
    content = f.read()

new_imports = """
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
"""

content = content.replace('import okhttp3.Request', new_imports + '\nimport okhttp3.Request')

classes_to_add = """
data class GeminiResult(
    val text: String? = null,
    val actions: List<String> = emptyList(),
    val editedFiles: List<Pair<String, Boolean>> = emptyList()
)

object GeminiClient"""

content = content.replace('object GeminiClient', classes_to_add)

old_fn = """    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key not configured. Please set it in AI Studio Secrets."
        }"""

new_fn = """    suspend fun generateContent(prompt: String): GeminiResult = suspendCancellableCoroutine { continuation ->
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            continuation.resume(GeminiResult(text = "Error: Gemini API Key not configured. Please set it in AI Studio Secrets."))
            return@suspendCancellableCoroutine
        }"""

content = content.replace(old_fn, new_fn)

old_fn_end = """        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val geminiResponse = responseAdapter.fromJson(responseBody)
                val part = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                
                if (part?.functionCall != null) {
                    val fnName = part.functionCall.name
                    val args = part.functionCall.args
                    if (fnName == "create_file" && args != null) {
                        val path = args["path"] as? String
                        val fileContent = args["content"] as? String
                        if (path != null && fileContent != null) {
                            val workspaceDir = LocalFileManager.getWorkspaceDir()
                            val targetFile = File(workspaceDir, path)
                            LocalFileManager.writeFile(targetFile, fileContent)
                            return@withContext "Created file: $path\\n\\n```\\n$fileContent\\n```"
                        }
                    }
                    return@withContext "Function called: $fnName"
                }
                
                part?.text ?: "No response text generated."
            } else {
                "API Error: ${response.code} - ${response.message}\\n$responseBody"
            }
        } catch (e: Exception) {
            "Network Error: ${e.message}"
        }
    }"""

new_fn_end = """        val call = client.newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                if (e.message?.contains("canceled", ignoreCase = true) == true || e.message?.contains("cancelled", ignoreCase = true) == true) {
                    continuation.resume(GeminiResult(text = "Generation stopped."))
                    return
                }
                continuation.resume(GeminiResult(text = "Network Error: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isCancelled) return
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val geminiResponse = responseAdapter.fromJson(responseBody)
                        val part = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                        
                        if (part?.functionCall != null) {
                            val fnName = part.functionCall.name
                            val args = part.functionCall.args
                            if (fnName == "create_file" && args != null) {
                                val path = args["path"] as? String
                                val fileContent = args["content"] as? String
                                if (path != null && fileContent != null) {
                                    kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                                        val workspaceDir = LocalFileManager.getWorkspaceDir()
                                        val targetFile = File(workspaceDir, path)
                                        LocalFileManager.writeFile(targetFile, fileContent)
                                    }
                                    continuation.resume(GeminiResult(
                                        actions = listOf("Created/Edited file: $path"),
                                        editedFiles = listOf(Pair(path, true))
                                    ))
                                    return
                                }
                            }
                            continuation.resume(GeminiResult(actions = listOf("Function called: $fnName")))
                            return
                        }
                        
                        continuation.resume(GeminiResult(text = part?.text ?: "No response text generated."))
                    } catch (e: Exception) {
                         continuation.resume(GeminiResult(text = "Error parsing response: ${e.message}"))
                    }
                } else {
                    continuation.resume(GeminiResult(text = "API Error: ${response.code} - ${response.message}\\n$responseBody"))
                }
            }
        })
    }"""

content = content.replace(old_fn_end, new_fn_end)

with open('app/src/main/java/com/example/ui/chat/GeminiClient.kt', 'w') as f:
    f.write(content)

