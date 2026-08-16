package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "request_logs")
data class RequestLogEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelName: String,
    val estimatedCost: Double,
    val timestamp: Long
)
