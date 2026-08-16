package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_ratings")
data class ModelRatingEntity(
    @PrimaryKey val messageId: String,
    val modelName: String,
    val providerId: String,
    val isPositive: Boolean,
    val timestamp: Long
)
