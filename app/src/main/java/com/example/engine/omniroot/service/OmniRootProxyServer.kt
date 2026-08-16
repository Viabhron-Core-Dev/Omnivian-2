package com.example.engine.omniroot.service

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import com.example.engine.omniroot.pipeline.TranslationEngine
import com.example.engine.omniroot.pipeline.CompressionEngine
import com.example.ui.chat.OmniRequest
import com.example.ui.chat.OmniResponse
import com.example.ui.chat.OmniMessage
import com.example.ui.chat.OmniChoice
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.example.engine.db.AppDatabase
import com.example.engine.db.TokenUsageEntity
import com.example.engine.db.RequestLogEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.json.JSONArray
import com.example.utils.LogKeeper

class OmniRootProxyServer(port: Int, private val context: Context) : NanoHTTPD("127.0.0.1", port) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val requestAdapter = moshi.adapter(OmniRequest::class.java)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    init {
        Log.d("OmniRootProxyServer", "Initializing OmniRoot Proxy on 127.0.0.1:$port")
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "")
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
            return response
        }

        if (session.uri == "/v1/chat/completions" && session.method == Method.POST) {
            try {
                val map = HashMap<String, String>()
                session.parseBody(map)
                val postData = map["postData"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing POST data")
                
                val request = requestAdapter.fromJson(postData) ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid JSON")
                val db = AppDatabase.getDatabase(context)
                
                val requestedModelStr = request.model ?: "google_ai_studio/gemini-1.5-pro-latest"
                
                // Determine the fallback chain (List of provider/model strings)
                val fallbackList = mutableListOf<String>()
                if (requestedModelStr.startsWith("fallback/")) {
                    val chainId = requestedModelStr.removePrefix("fallback/")
                    val chains = runBlocking { db.fallbackChainDao().getAllChains().first() }
                    val chain = chains.find { it.id == chainId }
                    if (chain != null) {
                        try {
                            val arr = JSONArray(chain.chainDataJson)
                            for (i in 0 until arr.length()) {
                                fallbackList.add(arr.getString(i))
                            }
                        } catch (e: Exception) {
                            Log.e("Proxy", "Error parsing chainDataJson", e)
                        }
                    }
                }
                
                if (fallbackList.isEmpty()) {
                    fallbackList.add(requestedModelStr)
                }

                // Try each model in the fallback chain
                var lastErrorResponse: String = "All providers failed."
                var lastCode = 500

                for (modelStr in fallbackList) {
                    val slashIdx = modelStr.indexOf('/')
                    val providerId = if (slashIdx != -1) modelStr.substring(0, slashIdx) else "google_ai_studio"
                    val actualModelName = if (slashIdx != -1) modelStr.substring(slashIdx + 1) else modelStr

                    // Determine target format and base url
                    val (targetFormat, baseUrl) = when (providerId) {
                        "google_ai_studio" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
                        "openai" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://api.openai.com/v1/chat/completions")
                        "anthropic" -> Pair(TranslationEngine.ProviderFormat.ANTHROPIC, "https://api.anthropic.com/v1/messages")
                        "openrouter" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://openrouter.ai/api/v1/chat/completions")
                        "groq" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://api.groq.com/openai/v1/chat/completions")
                        "together_ai" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://api.together.xyz/v1/chat/completions")
                        "local_gguf" -> Pair(TranslationEngine.ProviderFormat.OPENAI, "http://localhost:8080/v1/chat/completions")
                        else -> Pair(TranslationEngine.ProviderFormat.OPENAI, "https://api.openai.com/v1/chat/completions")
                    }

                    val activeKeys = runBlocking { db.apiKeyDao().getKeysForProvider(providerId).first() }
                    val key = activeKeys.firstOrNull { it.isActive }
                    
                    if (key == null && providerId != "local_gguf") {
                        lastErrorResponse = "No active API key found for $providerId"
                        continue // Try next in fallback chain
                    }


                    val updatedRequest = request.copy(model = actualModelName)
                    
                    if (providerId == "local_gguf") {
                        LogKeeper.log("Proxy", "Executing local inference via llama.cpp for model: $actualModelName", "")
                        
                        var combinedInputText = ""
                        updatedRequest.messages.forEach { combinedInputText += it.content + "\n" }
                        
                        // Retrieve the absolute path stored during import
                        val models = runBlocking { db.aiModelDao().getAllModels().first() }
                        val modelEntity = models.firstOrNull { it.providerId == "local_gguf" && it.modelId == actualModelName }
                        val absolutePath = modelEntity?.description ?: actualModelName
                        
                        val llama = com.example.engine.omniroot.local.LlamaEngine(context)
                        val loaded = llama.loadModelSafely(absolutePath)
                        
                        if (loaded) {
                            val prediction = llama.predict(combinedInputText)
                            llama.unloadModel()
                            
                            val localResponse = OmniResponse(
                                choices = listOf(
                                    OmniChoice(
                                        message = OmniMessage("assistant", prediction)
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

                    
                    // Compress and Translate
                    // In a real scenario we might compress messages here. For now just estimate tokens.
                    var combinedInputText = ""
                    updatedRequest.messages.forEach { combinedInputText += it.content + " " }
                    val inputTokens = CompressionEngine.estimateTokens(combinedInputText)

                    val translatedPayload = TranslationEngine.translateRequest(updatedRequest, targetFormat)
                    LogKeeper.log("Proxy", "Routing request to $providerId ($actualModelName)", translatedPayload)

                    val reqBuilder = Request.Builder()
                    when (targetFormat) {
                        TranslationEngine.ProviderFormat.GEMINI -> {
                            reqBuilder.url(baseUrl + "?key=" + (key?.keyValue ?: ""))
                        }
                        TranslationEngine.ProviderFormat.OPENAI -> {
                            reqBuilder.url(baseUrl)
                            reqBuilder.addHeader("Authorization", "Bearer " + (key?.keyValue ?: ""))
                            if (providerId == "openrouter") {
                                reqBuilder.addHeader("HTTP-Referer", "http://localhost:8080")
                                reqBuilder.addHeader("X-Title", "OmniRoot")
                            }
                        }
                        TranslationEngine.ProviderFormat.ANTHROPIC -> {
                            reqBuilder.url(baseUrl)
                            reqBuilder.addHeader("x-api-key", key?.keyValue ?: "")
                            reqBuilder.addHeader("anthropic-version", "2023-06-01")
                        }
                    }

                    val req = reqBuilder.post(translatedPayload.toRequestBody("application/json".toMediaType())).build()

                    try {
                        val response = httpClient.newCall(req).execute()
                        val responseBody = response.body?.string() ?: ""

                        if (!response.isSuccessful) {
                            LogKeeper.log("Proxy Error", "API Error from $providerId", "Code: ${response.code}\nBody: $responseBody")
                            lastErrorResponse = "API Error ${response.code}: $responseBody"
                            lastCode = response.code
                            
                            // If 429 or 5xx, we continue to the next provider in the chain. 
                            if (response.code == 429 || response.code >= 500) {
                                continue
                            } else {
                                // For 400 Bad Request, continuing might just fail again. But let's continue anyway for robustness.
                                continue
                            }
                        }

                        LogKeeper.log("Proxy", "Received response from $providerId", "Code: ${response.code}\nBody length: ${responseBody.length}")

                        var standardResponse = TranslationEngine.translateResponse(responseBody, targetFormat)
                        
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
                                                    content = "Tool executed: $funcName\nResult: $result"
                                                )
                                            )
                                        )
                                    )
                                    break
                                }
                            }
                        }
                        
                        // Calculate tokens and cost
                        val outputText = standardResponse.choices?.firstOrNull()?.message?.content ?: ""
                        val outputTokens = CompressionEngine.estimateTokens(outputText)
                        val totalTokens = inputTokens + outputTokens
                        val estimatedCost = CompressionEngine.estimateCost(providerId, actualModelName, inputTokens, outputTokens)
                        
                        runBlocking {
                            val timestamp = System.currentTimeMillis()
                            db.metricsDao().insertTokenUsage(TokenUsageEntity(id = UUID.randomUUID().toString(), providerId = providerId, modelName = actualModelName, tokensUsed = totalTokens, timestamp = timestamp))
                            db.metricsDao().insertRequestLog(RequestLogEntity(id = UUID.randomUUID().toString(), providerId = providerId, modelName = actualModelName, estimatedCost = estimatedCost, timestamp = timestamp))
                        }

                        // Override the standard response model name to let UI know exactly which model in the chain succeeded
                        val finalResponse = standardResponse
                        val moshiResponse = Moshi.Builder().build().adapter(com.example.ui.chat.OmniResponse::class.java).toJson(finalResponse)
                        
                        val res = newFixedLengthResponse(Response.Status.OK, "application/json", moshiResponse)
                        res.addHeader("Access-Control-Allow-Origin", "*")
                        return res

                    } catch (e: Exception) {
                        LogKeeper.log("Proxy Error", "Exception in proxy for $providerId", e.message ?: "Unknown", e.stackTraceToString())
                        lastErrorResponse = "Proxy Exception: ${e.message}"
                        continue // Try next
                    }
                }

                // If we get here, the entire fallback chain failed
                val errorResponse = com.example.ui.chat.OmniResponse(choices = listOf(com.example.ui.chat.OmniChoice(message = com.example.ui.chat.OmniMessage(role = "assistant", content = lastErrorResponse))))
                val errorJson = Moshi.Builder().build().adapter(com.example.ui.chat.OmniResponse::class.java).toJson(errorResponse)
                val res = newFixedLengthResponse(Response.Status.OK, "application/json", errorJson)
                res.addHeader("Access-Control-Allow-Origin", "*")
                return res

            } catch (e: Exception) {
                Log.e("OmniRootProxyServer", "Error processing request", e)
                LogKeeper.log("Proxy Error", "Exception in proxy", e.message ?: "Unknown error", e.stackTraceToString())
                val errorResponse = com.example.ui.chat.OmniResponse(choices = listOf(com.example.ui.chat.OmniChoice(message = com.example.ui.chat.OmniMessage(role = "assistant", content = "Proxy Exception: ${e.message}"))))
                val errorJson = Moshi.Builder().build().adapter(com.example.ui.chat.OmniResponse::class.java).toJson(errorResponse)
                val res = newFixedLengthResponse(Response.Status.OK, "application/json", errorJson)
                res.addHeader("Access-Control-Allow-Origin", "*")
                return res
            }
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
}
