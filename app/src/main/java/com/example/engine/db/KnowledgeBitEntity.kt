package com.example.engine.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "knowledge_bits")
data class KnowledgeBitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val sourceUrl: String? = null,
    val contentType: String = "NOTE", // CODE, ARTICLE, TABLE, PRESENTATION, NOTE, DOCUMENT
    val originalTimestamp: Long = System.currentTimeMillis(),
    val cachedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 1,
    val isPinned: Boolean = false,
    val ttlSeconds: Long = 86400L, // 24h default for temp bits, 0 for infinite
    val workspaceId: String? = null,
    val summary: String? = null
)

@Dao
interface KnowledgeBitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(bit: KnowledgeBitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBits(bits: List<KnowledgeBitEntity>)

    @Query("SELECT * FROM knowledge_bits ORDER BY isPinned DESC, lastAccessedAt DESC")
    fun getAllBits(): Flow<List<KnowledgeBitEntity>>

    @Query("SELECT * FROM knowledge_bits WHERE isPinned = 1 ORDER BY lastAccessedAt DESC")
    fun getPinnedBits(): Flow<List<KnowledgeBitEntity>>

    @Query("SELECT * FROM knowledge_bits ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun getRecentBits(limit: Int = 20): Flow<List<KnowledgeBitEntity>>

    @Query("SELECT * FROM knowledge_bits WHERE id = :id LIMIT 1")
    suspend fun getBitById(id: String): KnowledgeBitEntity?

    @Query("SELECT * FROM knowledge_bits WHERE sourceUrl = :url LIMIT 1")
    suspend fun getBitByUrl(url: String): KnowledgeBitEntity?

    @Query("SELECT * FROM knowledge_bits WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY isPinned DESC, accessCount DESC, lastAccessedAt DESC")
    suspend fun searchBits(query: String): List<KnowledgeBitEntity>

    @Query("UPDATE knowledge_bits SET accessCount = accessCount + 1, lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun incrementAccess(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE knowledge_bits SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)

    @Query("DELETE FROM knowledge_bits WHERE id = :id")
    suspend fun deleteBit(id: String)

    @Query("DELETE FROM knowledge_bits WHERE isPinned = 0 AND ttlSeconds > 0 AND (cachedAt + (ttlSeconds * 1000)) < :currentTime")
    suspend fun pruneExpired(currentTime: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM knowledge_bits WHERE isPinned = 0")
    suspend fun clearTemporary()
}
