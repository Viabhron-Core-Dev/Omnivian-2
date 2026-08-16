package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_configs")
data class WorkspaceConfigEntity(
    @PrimaryKey
    val workspaceId: String,
    val threadName: String,
    val appType: String,
    val model: String,
    val integrations: String,
    val instructions: String
)
