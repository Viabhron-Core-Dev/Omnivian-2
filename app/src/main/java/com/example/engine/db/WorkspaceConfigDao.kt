package com.example.engine.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkspaceConfigDao {
    @Query("SELECT * FROM workspace_configs WHERE workspaceId = :workspaceId")
    suspend fun getConfig(workspaceId: String): WorkspaceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: WorkspaceConfigEntity)

    @Query("DELETE FROM workspace_configs WHERE workspaceId = :workspaceId")
    suspend fun deleteConfig(workspaceId: String)
}
