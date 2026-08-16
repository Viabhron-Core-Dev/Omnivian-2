package com.example.engine.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSettingsDao {
    @Query("SELECT * FROM chat_settings WHERE workspaceId = :workspaceId")
    fun getSettingsFlow(workspaceId: String): Flow<ChatSettingsEntity?>

    @Query("SELECT * FROM chat_settings WHERE workspaceId = :workspaceId")
    suspend fun getSettings(workspaceId: String): ChatSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ChatSettingsEntity)
}
