package com.example.engine.db

import android.database.Cursor
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "COLOR_NOTES", "HTML", "REACT", "PWA"
    val content: String, // JSON payload for notes, or raw HTML / code
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val workspaceId: String? = null,
    val iconUri: String? = null,
    val isLightweight: Boolean = false,
    val manifestJson: String? = null,
    val settingsJson: String? = null,
    val version: Long = 1L
)

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllArtifactsFlow(): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllArtifacts(): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts WHERE id = :id LIMIT 1")
    suspend fun getArtifactById(id: String): ArtifactEntity?

    @Query("SELECT * FROM artifacts WHERE type IN ('HTML', 'PWA', 'REACT') ORDER BY isPinned DESC, updatedAt DESC")
    fun getPwaArtifactsCursor(): Cursor

    @Query("SELECT * FROM artifacts WHERE id = :id LIMIT 1")
    fun getArtifactCursorById(id: String): Cursor

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtifacts(artifacts: List<ArtifactEntity>)

    @Update
    suspend fun updateArtifact(artifact: ArtifactEntity)

    @Delete
    suspend fun deleteArtifact(artifact: ArtifactEntity)

    @Query("DELETE FROM artifacts WHERE id = :id")
    suspend fun deleteById(id: String)
}
