package com.example.engine.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.example.ai.IAiCallback
import com.example.ai.IAiInferenceService
import com.example.engine.omniroot.local.LocalAiManager
import com.example.ui.chat.ChatMessage
import com.example.ui.chat.MessageRole
import com.example.ui.chat.OmniRootClient
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class AiInferenceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val binder = object : IAiInferenceService.Stub() {
        override fun streamPrompt(
            modelId: String?,
            systemPrompt: String?,
            userPrompt: String?,
            options: Bundle?,
            callback: IAiCallback?
        ) {
            if (userPrompt.isNullOrBlank() || callback == null) {
                callback?.onError(400, "User prompt is empty or callback is null")
                return
            }

            val requestId = options?.getString("requestId") ?: System.currentTimeMillis().toString()

            val job = serviceScope.launch {
                try {
                    val targetModel = modelId?.ifBlank { "gemini-2.5-flash" } ?: "gemini-2.5-flash"
                    val fullResponseBuilder = StringBuilder()

                    val messages = mutableListOf<ChatMessage>()
                    if (!systemPrompt.isNullOrBlank()) {
                        messages.add(ChatMessage(role = MessageRole.USER, text = "System Instruction: $systemPrompt"))
                        messages.add(ChatMessage(role = MessageRole.AI, text = "Understood. I will follow your instructions."))
                    }
                    messages.add(ChatMessage(role = MessageRole.USER, text = userPrompt))

                    // Execute generation via OmniRoot gateway
                    val result = OmniRootClient.generateContent(
                        messages = messages,
                        model = targetModel
                    )

                    val text = result.text ?: "No response generated."
                    fullResponseBuilder.append(text)
                    try {
                        callback.onToken(text)
                    } catch (_: Exception) {}

                    val meta = Bundle().apply {
                        putString("modelId", targetModel)
                        putInt("totalLength", fullResponseBuilder.length)
                    }
                    callback.onComplete(fullResponseBuilder.toString(), meta)
                } catch (e: CancellationException) {
                    // Canceled by client
                } catch (e: Exception) {
                    callback.onError(500, e.message ?: "Unknown inference error")
                } finally {
                    activeJobs.remove(requestId)
                }
            }

            activeJobs[requestId] = job
        }

        override fun cancelInference(requestId: String?) {
            if (requestId != null) {
                activeJobs.remove(requestId)?.cancel()
            }
        }

        override fun getAvailableModels(): List<String> {
            return listOf(
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-1.5-flash",
                "claude-3-5-sonnet",
                "gpt-4o",
                "local/on-device.gguf"
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        activeJobs.clear()
    }
}

