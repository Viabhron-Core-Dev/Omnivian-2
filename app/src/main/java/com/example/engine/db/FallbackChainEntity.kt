package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fallback_chains")
data class FallbackChainEntity(
    @PrimaryKey val id: String,
    val name: String,
    val chainDataJson: String, // Serialized list of ApiKey IDs or Provider IDs
    val createdAt: Long
)
