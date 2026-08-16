package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_usage")
data class TokenUsageEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelName: String,
    val tokensUsed: Int,
    val timestamp: Long
)
