package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_settings")
data class ChatSettingsEntity(
    @PrimaryKey val workspaceId: String,
    val temperature: Float = 0.7f,
    val minP: Float = 0.05f,
    val topP: Float = 0.95f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "",
    val contextSize: Int = 2048,
    val numThreads: Int = 4,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
    val unfoldOnScreen: Boolean = false
)
