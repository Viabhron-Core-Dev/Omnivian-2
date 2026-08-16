import re

# Update OmniRouteClient.kt
new_omni_content = """package com.example.ui.chat

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

@JsonClass(generateAdapter = true)
data class OmniMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OmniRequest(
    val model: String = "omni-default",
    val messages: List<OmniMessage>
)

@JsonClass(generateAdapter = true)
data class OmniResponse(
    val choices: List<OmniChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OmniChoice(
    val message: OmniMessage? = null
)

data class OmniRouteResult(
    val text: String? = null,
    val actions: List<String> = emptyList(),
    val editedFiles: List<Pair<String, Boolean>> = emptyList()
)

object OmniRouteClient {
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

    suspend fun generateContent(messages: List<ChatMessage>): OmniRouteResult = suspendCancellableCoroutine { continuation ->
        
        val omniMessages = messages.mapNotNull { msg ->
            when (msg.role) {
                MessageRole.USER -> OmniMessage(role = "user", content = msg.text)
                MessageRole.AI -> OmniMessage(role = "assistant", content = msg.text)
                else -> null
            }
        }
        
        val requestData = OmniRequest(
            messages = omniMessages
        )
        
        val jsonBody = requestAdapter.toJson(requestData)
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(baseUrl)
            .post(body)
            .build()
            
        val call = client.newCall(request)
        
        continuation.invokeOnCancellation {
            call.cancel()
        }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                if (e.message?.contains("canceled", ignoreCase = true) == true || e.message?.contains("cancelled", ignoreCase = true) == true) {
                    continuation.resume(OmniRouteResult(text = "Generation stopped."))
                    return
                }
                continuation.resume(OmniRouteResult(text = "Network Error: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isCancelled) return
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val omniResponse = responseAdapter.fromJson(responseBody)
                        val text = omniResponse?.choices?.firstOrNull()?.message?.content
                        continuation.resume(OmniRouteResult(text = text ?: "No response generated."))
                    } catch (e: Exception) {
                         continuation.resume(OmniRouteResult(text = "Error parsing response: ${e.message}"))
                    }
                } else {
                    continuation.resume(OmniRouteResult(text = "API Error: ${response.code} - ${response.message}\\n$responseBody"))
                }
            }
        })
    }
}
"""
with open('app/src/main/java/com/example/ui/chat/OmniRouteClient.kt', 'w') as f:
    f.write(new_omni_content)

# Update ChatScreen.kt
with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    chat_content = f.read()

chat_content = chat_content.replace(
    'val response = com.example.ui.chat.OmniRouteClient.generateContent(prompt)',
    'val response = com.example.ui.chat.OmniRouteClient.generateContent(chatMessages.filter { it.id != generatingMessage.id })'
)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(chat_content)

