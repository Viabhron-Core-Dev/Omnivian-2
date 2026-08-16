package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_pull_requests")
data class WorkspacePullRequestEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val description: String,
    val targetFile: String?,
    val diff: String,
    val status: String, // "open", "merged", "closed"
    val createdAt: Long
)
