package com.example.engine.db

import com.example.ui.chat.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

fun ChatMessage.toEntity(sessionId: String): ChatMessageEntity {
    val editedFilesJson = JSONArray()
    this.editedFiles.forEach { pair ->
        val obj = JSONObject()
        obj.put("path", pair.first)
        obj.put("status", pair.second)
        editedFilesJson.put(obj)
    }

    val appActionsJson = JSONArray()
    this.appActions.forEach { action ->
        appActionsJson.put(action)
    }


    return ChatMessageEntity(
        id = this.id,
        sessionId = sessionId,
        text = this.text,
        role = this.role,
        modelName = this.modelName,
        providerId = this.providerId,
        editedFilesJson = editedFilesJson.toString(),
        appActionsJson = appActionsJson.toString(),
        isFolded = this.isFolded,
        timestamp = System.currentTimeMillis()
    )

}

fun ChatMessageEntity.toDomainModel(): ChatMessage {
    val editedFiles = mutableListOf<Pair<String, Boolean>>()
    try {
        val array = JSONArray(this.editedFilesJson)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            editedFiles.add(Pair(obj.getString("path"), obj.getBoolean("status")))
        }
    } catch (e: Exception) {
        // Ignore JSON parsing errors
    }

    val appActions = mutableListOf<String>()
    try {
        val array = JSONArray(this.appActionsJson)
        for (i in 0 until array.length()) {
            appActions.add(array.getString(i))
        }
    } catch (e: Exception) {
        // Ignore JSON parsing errors
    }


    return ChatMessage(
        id = this.id,
        text = this.text,
        role = this.role,
        modelName = this.modelName,
        providerId = this.providerId,
        editedFiles = editedFiles,
        appActions = appActions,
        isFolded = this.isFolded
    )

}
