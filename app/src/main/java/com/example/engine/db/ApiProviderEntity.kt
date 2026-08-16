package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_providers")
data class ApiProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val loginUrl: String,
    val isFreeTierAvailable: Boolean,
    val description: String
)
