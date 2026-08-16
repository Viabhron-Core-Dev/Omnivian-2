package com.example.engine.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceIssueDao {
    @Query("SELECT * FROM workspace_issues WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getIssuesForWorkspace(workspaceId: String): Flow<List<WorkspaceIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveIssue(issue: WorkspaceIssueEntity)
}
