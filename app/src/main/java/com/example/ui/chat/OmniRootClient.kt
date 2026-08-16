package com.example.ui.chat

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.utils.LogKeeper

@JsonClass(generateAdapter = true)
data class OmniMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<OmniToolCall>? = null,
    val tool_call_id: String? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class OmniToolCall(
    val id: String,
    val type: String = "function",
    val function: OmniFunctionCall
)

@JsonClass(generateAdapter = true)
data class OmniFunctionCall(
    val name: String,
    val arguments: String
)

@JsonClass(generateAdapter = true)
data class OmniTool(
    val type: String = "function",
    val function: OmniFunctionDef
)

@JsonClass(generateAdapter = true)
data class OmniFunctionDef(
    val name: String,
    val description: String? = null,
    val parameters: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class OmniRequest(
    val model: String = "omni-default",
    val messages: List<OmniMessage>,
    val tools: List<OmniTool>? = null,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val max_tokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OmniResponse(
    val choices: List<OmniChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OmniChoice(
    val message: OmniMessage? = null
)

data class OmniRootResult(
    val text: String? = null,
    val actions: List<String> = emptyList(),
    val editedFiles: List<Pair<String, Boolean>> = emptyList()
)

object OmniRootClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(OmniRequest::class.java)
    private val responseAdapter = moshi.adapter(OmniResponse::class.java)
    
    var baseUrl: String = "http://localhost:8080/v1/chat/completions"

    suspend fun generateContent(
        messages: List<ChatMessage>, 
        model: String = "omni-default",
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): OmniRootResult = suspendCancellableCoroutine { continuation ->
        
        val omniMessages = messages.mapNotNull { msg ->
            when (msg.role) {
                MessageRole.USER -> OmniMessage(role = "user", content = msg.text)
                MessageRole.AI -> OmniMessage(role = "assistant", content = msg.text)
                else -> null
            }
        }
        
        val requestData = OmniRequest(
            model = model,
            messages = omniMessages,
            temperature = temperature,
            top_p = topP,
            max_tokens = maxTokens
        )
        
        val jsonBody = requestAdapter.toJson(requestData)
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(baseUrl)
            .post(body)
            .build()
            
        LogKeeper.log("Network", "Request to $baseUrl", jsonBody)
            
        val call = client.newCall(request)
        
        continuation.invokeOnCancellation {
            call.cancel()
        }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogKeeper.log("Network Error", "Request failed", "${e.message}", e.stackTraceToString())
                if (continuation.isCancelled) return
                if (e.message?.contains("canceled", ignoreCase = true) == true || e.message?.contains("cancelled", ignoreCase = true) == true) {
                    continuation.resume(OmniRootResult(text = "Generation stopped."))
                    return
                }
                continuation.resume(OmniRootResult(text = "Network Error: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                LogKeeper.log("Network", "Response code: ${response.code}", responseBody ?: "null")
                if (continuation.isCancelled) return
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val omniResponse = responseAdapter.fromJson(responseBody)
                        val text = omniResponse?.choices?.firstOrNull()?.message?.content
                        continuation.resume(OmniRootResult(text = text ?: "No response generated."))
                    } catch (e: Exception) {
                         continuation.resume(OmniRootResult(text = "Error parsing response: ${e.message}"))
                    }
                } else {
                    continuation.resume(OmniRootResult(text = "API Error: ${response.code} - ${response.message}\n$responseBody"))
                }
            }
        })
    }
}
