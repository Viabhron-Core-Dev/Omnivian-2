package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "api_keys",
    foreignKeys = [
        ForeignKey(
            entity = ApiProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"])]
)
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val alias: String,
    val keyMasked: String,
    val keyValue: String, // In a real app, this should be encrypted via Android Keystore
    val isActive: Boolean,
    val createdAt: Long
)
