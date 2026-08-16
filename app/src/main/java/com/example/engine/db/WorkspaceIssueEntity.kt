package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_issues")
data class WorkspaceIssueEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val description: String,
    val targetFile: String?,
    val status: String, // "open", "closed"
    val createdAt: Long
)
