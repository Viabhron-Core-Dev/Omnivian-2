package com.example.engine.db

import androidx.room.Entity

@Entity(tableName = "ai_models", primaryKeys = ["providerId", "modelId"])
data class AiModelEntity(
    val providerId: String,
    val modelId: String,
    val fetchedAt: Long = System.currentTimeMillis(),
    val inputType: String = "TEXT", // TEXT, AUDIO, IMAGE, MULTIMODAL
    val outputType: String = "TEXT", // TEXT, AUDIO, EMBEDDING, UNSUPPORTED
    val description: String? = null
)