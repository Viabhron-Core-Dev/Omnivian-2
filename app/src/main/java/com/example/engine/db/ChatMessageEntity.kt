package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.chat.MessageRole
import com.example.ui.chat.ChatMessage
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val text: String,
    val role: MessageRole,
    val modelName: String? = null,
    val providerId: String? = null,
    val editedFilesJson: String,

    val appActionsJson: String,
    val isFolded: Boolean = true,
    val timestamp: Long
)
