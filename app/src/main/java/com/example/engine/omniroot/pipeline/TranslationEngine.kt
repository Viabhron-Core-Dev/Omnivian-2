package com.example.engine.omniroot.pipeline

import com.example.ui.chat.OmniMessage
import com.example.ui.chat.OmniRequest
import com.example.ui.chat.OmniResponse
import com.example.ui.chat.OmniChoice
import com.example.ui.chat.OmniToolCall
import com.example.ui.chat.OmniFunctionCall
import org.json.JSONArray
import org.json.JSONObject

object TranslationEngine {

    enum class ProviderFormat {
        OPENAI,
        ANTHROPIC,
        GEMINI
    }

    fun translateRequest(request: OmniRequest, targetFormat: ProviderFormat, context: android.content.Context? = null): String {
        val audioRegex = Regex("\\[Audio:\\s*([a-zA-Z0-9_\\-\\.]+)\\]")
        return when (targetFormat) {
            ProviderFormat.OPENAI -> {
                val json = JSONObject()
                json.put("model", request.model)
                val messagesArray = JSONArray()
                request.messages.forEach { msg ->
                    val msgObj = JSONObject()
                    msgObj.put("role", msg.role)
                    if (msg.name != null) msgObj.put("name", msg.name)
                    if (msg.tool_call_id != null) msgObj.put("tool_call_id", msg.tool_call_id)
                    
                    val contentStr = msg.content ?: ""
                    val audioMatch = audioRegex.find(contentStr)
                    val audioFileName = audioMatch?.groupValues?.getOrNull(1)
                    val audioFile = if (context != null && audioFileName != null) {
                        java.io.File(context.filesDir, "recordings/$audioFileName")
                    } else null

                    if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                        val contentArray = JSONArray()
                        val cleanText = contentStr.replace(audioRegex, "").trim()
                        if (cleanText.isNotBlank()) {
                            contentArray.put(JSONObject().apply {
                                put("type", "text")
                                put("text", cleanText)
                            })
                        } else {
                            contentArray.put(JSONObject().apply {
                                put("type", "text")
                                put("text", "Please listen to this voice audio input and answer:")
                            })
                        }
                        try {
                            val audioBytes = audioFile.readBytes()
                            val b64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
                            val audioPart = JSONObject().apply {
                                put("type", "input_audio")
                                put("input_audio", JSONObject().apply {
                                    put("data", b64)
                                    put("format", "mp3")
                                })
                            }
                            contentArray.put(audioPart)
                        } catch (_: Exception) {}
                        msgObj.put("content", contentArray)
                    } else {
                        if (msg.content != null) msgObj.put("content", msg.content)
                    }
                    
                    if (msg.tool_calls != null) {
                        val tcArray = JSONArray()
                        msg.tool_calls.forEach { tc ->
                            val tcObj = JSONObject()
                            tcObj.put("id", tc.id)
                            tcObj.put("type", tc.type)
                            val fObj = JSONObject()
                            fObj.put("name", tc.function.name)
                            fObj.put("arguments", tc.function.arguments)
                            tcObj.put("function", fObj)
                            tcArray.put(tcObj)
                        }
                        msgObj.put("tool_calls", tcArray)
                    }
                    messagesArray.put(msgObj)
                }
                json.put("messages", messagesArray)
                
                if (request.tools != null && request.tools.isNotEmpty()) {
                    val toolsArray = JSONArray()
                    request.tools.forEach { tool ->
                        val tObj = JSONObject()
                        tObj.put("type", tool.type)
                        val fObj = JSONObject()
                        fObj.put("name", tool.function.name)
                        if (tool.function.description != null) fObj.put("description", tool.function.description)
                        if (tool.function.parameters != null) fObj.put("parameters", JSONObject(tool.function.parameters))
                        tObj.put("function", fObj)
                        toolsArray.put(tObj)
                    }
                    json.put("tools", toolsArray)
                }
                
                if (request.temperature != null) json.put("temperature", request.temperature)
                if (request.top_p != null) json.put("top_p", request.top_p)
                if (request.max_tokens != null) json.put("max_tokens", request.max_tokens)

                json.toString(2)
            }
            ProviderFormat.ANTHROPIC -> {
                val json = JSONObject()
                json.put("model", request.model)
                json.put("max_tokens", request.max_tokens ?: 4096)
                if (request.temperature != null) json.put("temperature", request.temperature)
                if (request.top_p != null) json.put("top_p", request.top_p)
                
                var systemPrompt = ""
                val messagesArray = JSONArray()
                request.messages.forEach { msg ->
                    if (msg.role == "system") {
                        systemPrompt += (msg.content ?: "") + "\n"
                    } else {
                        val msgObj = JSONObject()
                        val role = if (msg.role == "user") "user" else "assistant"
                        msgObj.put("role", role)
                        
                        val contentArray = JSONArray()
                        if (msg.content != null) {
                            val textObj = JSONObject()
                            textObj.put("type", "text")
                            textObj.put("text", msg.content)
                            contentArray.put(textObj)
                        }
                        if (msg.tool_calls != null) {
                            msg.tool_calls.forEach { tc ->
                                val tcObj = JSONObject()
                                tcObj.put("type", "tool_use")
                                tcObj.put("id", tc.id)
                                tcObj.put("name", tc.function.name)
                                tcObj.put("input", JSONObject(tc.function.arguments))
                                contentArray.put(tcObj)
                            }
                        }
                        if (msg.role == "tool") {
                            // Anthropic tool result mapping
                            msgObj.put("role", "user")
                            val trObj = JSONObject()
                            trObj.put("type", "tool_result")
                            trObj.put("tool_use_id", msg.tool_call_id)
                            trObj.put("content", msg.content)
                            contentArray.put(trObj)
                        }
                        msgObj.put("content", contentArray)
                        messagesArray.put(msgObj)
                    }
                }
                
                if (systemPrompt.isNotEmpty()) {
                    json.put("system", systemPrompt.trim())
                }
                
                if (request.tools != null && request.tools.isNotEmpty()) {
                    val toolsArray = JSONArray()
                    request.tools.forEach { tool ->
                        val tObj = JSONObject()
                        tObj.put("name", tool.function.name)
                        if (tool.function.description != null) tObj.put("description", tool.function.description)
                        if (tool.function.parameters != null) {
                            tObj.put("input_schema", JSONObject(tool.function.parameters))
                        }
                        toolsArray.put(tObj)
                    }
                    json.put("tools", toolsArray)
                }
                
                json.put("messages", messagesArray)
                json.toString(2)
            }
            ProviderFormat.GEMINI -> {
                val json = JSONObject()
                val contentsArray = JSONArray()
                
                var systemInstruction = ""
                
                request.messages.forEach { msg ->
                    if (msg.role == "system") {
                        systemInstruction += (msg.content ?: "") + "\n"
                    } else if (msg.role == "tool") {
                        val contentObj = JSONObject()
                        contentObj.put("role", "user")
                        val partsArray = JSONArray()
                        val partObj = JSONObject()
                        
                        val funcResObj = JSONObject()
                        funcResObj.put("name", msg.name ?: "")
                        
                        val responseObj = JSONObject()
                        try {
                            val jsonObj = JSONObject(msg.content ?: "{}")
                            // Pass parsed json if valid
                            responseObj.put("name", msg.name)
                            responseObj.put("response", jsonObj)
                        } catch (e: Exception) {
                            val fallObj = JSONObject()
                            fallObj.put("result", msg.content)
                            responseObj.put("name", msg.name)
                            responseObj.put("response", fallObj)
                        }
                        
                        funcResObj.put("response", responseObj.optJSONObject("response") ?: JSONObject())
                        partObj.put("functionResponse", funcResObj)
                        partsArray.put(partObj)
                        contentObj.put("parts", partsArray)
                        contentsArray.put(contentObj)
                        
                    } else {
                        val contentObj = JSONObject()
                        val role = if (msg.role == "user") "user" else "model"
                        contentObj.put("role", role)
                        val partsArray = JSONArray()
                        
                        val contentStr = msg.content ?: ""
                        val audioMatch = audioRegex.find(contentStr)
                        val audioFileName = audioMatch?.groupValues?.getOrNull(1)
                        val audioFile = if (context != null && audioFileName != null) {
                            java.io.File(context.filesDir, "recordings/$audioFileName")
                        } else null

                        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                            val cleanText = contentStr.replace(audioRegex, "").trim()
                            if (cleanText.isNotBlank()) {
                                partsArray.put(JSONObject().apply { put("text", cleanText) })
                            }
                            try {
                                val audioBytes = audioFile.readBytes()
                                val b64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", b64)
                                }
                                partsArray.put(JSONObject().apply { put("inlineData", inlineData) })
                            } catch (_: Exception) {}
                        } else {
                            if (msg.content != null) {
                                val partObj = JSONObject()
                                partObj.put("text", msg.content)
                                partsArray.put(partObj)
                            }
                        }
                        if (msg.tool_calls != null) {
                            msg.tool_calls.forEach { tc ->
                                val callObj = JSONObject()
                                val funcCallObj = JSONObject()
                                funcCallObj.put("name", tc.function.name)
                                funcCallObj.put("args", JSONObject(tc.function.arguments))
                                callObj.put("functionCall", funcCallObj)
                                partsArray.put(callObj)
                            }
                        }
                        
                        contentObj.put("parts", partsArray)
                        contentsArray.put(contentObj)
                    }
                }
                
                if (systemInstruction.isNotEmpty()) {
                    val sysInstructionObj = JSONObject()
                    val sysPartsArray = JSONArray()
                    val sysPartObj = JSONObject()
                    sysPartObj.put("text", systemInstruction.trim())
                    sysPartsArray.put(sysPartObj)
                    sysInstructionObj.put("parts", sysPartsArray)
                    json.put("systemInstruction", sysInstructionObj)
                }
                
                if (request.tools != null && request.tools.isNotEmpty()) {
                    val funcsArray = JSONArray()
                    request.tools.forEach { tool ->
                        val fObj = JSONObject()
                        fObj.put("name", tool.function.name)
                        if (tool.function.description != null) fObj.put("description", tool.function.description)
                        if (tool.function.parameters != null) {
                            fObj.put("parameters", JSONObject(tool.function.parameters))
                        }
                        funcsArray.put(fObj)
                    }
                    val toolsArray = JSONArray()
                    val toolsObj = JSONObject()
                    toolsObj.put("functionDeclarations", funcsArray)
                    toolsArray.put(toolsObj)
                    json.put("tools", toolsArray)
                }

                if (request.temperature != null || request.top_p != null || request.max_tokens != null) {
                    val genConfig = JSONObject()
                    if (request.temperature != null) genConfig.put("temperature", request.temperature)
                    if (request.top_p != null) genConfig.put("topP", request.top_p)
                    if (request.max_tokens != null) genConfig.put("maxOutputTokens", request.max_tokens)
                    json.put("generationConfig", genConfig)
                }
                
                json.put("contents", contentsArray)
                json.toString(2)
            }
        }
    }

    fun translateResponse(rawResponse: String, sourceFormat: ProviderFormat): OmniResponse {
        return try {
            val json = JSONObject(rawResponse)
            when (sourceFormat) {
                ProviderFormat.OPENAI -> {
                    val choices = json.optJSONArray("choices")
                    val messageObj = choices?.optJSONObject(0)?.optJSONObject("message")
                    val content = messageObj?.optString("content", null)
                    
                    val toolCallsArray = messageObj?.optJSONArray("tool_calls")
                    var toolsList: MutableList<OmniToolCall>? = null
                    if (toolCallsArray != null) {
                        toolsList = mutableListOf()
                        for (i in 0 until toolCallsArray.length()) {
                            val t = toolCallsArray.optJSONObject(i)
                            val f = t.optJSONObject("function")
                            if (t != null && f != null) {
                                toolsList.add(OmniToolCall(
                                    id = t.optString("id"),
                                    type = t.optString("type", "function"),
                                    function = OmniFunctionCall(name = f.optString("name"), arguments = f.optString("arguments"))
                                ))
                            }
                        }
                    }
                    
                    OmniResponse(choices = listOf(OmniChoice(message = OmniMessage(role = "assistant", content = content, tool_calls = toolsList))))
                }
                ProviderFormat.ANTHROPIC -> {
                    val contentArray = json.optJSONArray("content")
                    var textContent: String? = null
                    var toolsList: MutableList<OmniToolCall>? = null
                    
                    if (contentArray != null) {
                        for (i in 0 until contentArray.length()) {
                            val c = contentArray.optJSONObject(i)
                            if (c.optString("type") == "text") {
                                textContent = c.optString("text")
                            } else if (c.optString("type") == "tool_use") {
                                if (toolsList == null) toolsList = mutableListOf()
                                toolsList.add(OmniToolCall(
                                    id = c.optString("id"),
                                    type = "function",
                                    function = OmniFunctionCall(name = c.optString("name"), arguments = c.optJSONObject("input")?.toString() ?: "{}")
                                ))
                            }
                        }
                    }
                    OmniResponse(choices = listOf(OmniChoice(message = OmniMessage(role = "assistant", content = textContent, tool_calls = toolsList))))
                }
                ProviderFormat.GEMINI -> {
                    val candidates = json.optJSONArray("candidates")
                    val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    
                    var textContent: String? = null
                    var toolsList: MutableList<OmniToolCall>? = null
                    
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val p = parts.optJSONObject(i)
                            if (p.has("text")) {
                                textContent = p.optString("text")
                            } else if (p.has("functionCall")) {
                                val fCall = p.optJSONObject("functionCall")
                                if (fCall != null) {
                                    if (toolsList == null) toolsList = mutableListOf()
                                    toolsList.add(OmniToolCall(
                                        id = "call_" + java.util.UUID.randomUUID().toString().substring(0,8), // Gemini doesn't provide IDs
                                        type = "function",
                                        function = OmniFunctionCall(name = fCall.optString("name"), arguments = fCall.optJSONObject("args")?.toString() ?: "{}")
                                    ))
                                }
                            }
                        }
                    }
                    
                    OmniResponse(choices = listOf(OmniChoice(message = OmniMessage(role = "assistant", content = textContent, tool_calls = toolsList))))
                }
            }
        } catch (e: Exception) {
            OmniResponse(choices = listOf(OmniChoice(message = OmniMessage(role = "assistant", content = "Error translating response: ${e.message}"))))
        }
    }
}
