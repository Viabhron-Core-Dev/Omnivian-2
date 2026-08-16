package com.example.engine.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspacePullRequestDao {
    @Query("SELECT * FROM workspace_pull_requests WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getPullRequestsForWorkspace(workspaceId: String): Flow<List<WorkspacePullRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePullRequest(pr: WorkspacePullRequestEntity)
}
