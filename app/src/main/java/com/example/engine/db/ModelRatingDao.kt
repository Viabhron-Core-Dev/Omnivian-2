package com.example.engine.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: ModelRatingEntity)

    @Query("SELECT * FROM model_ratings")
    fun getAllRatings(): Flow<List<ModelRatingEntity>>
    
    @Query("SELECT modelName, providerId, SUM(CASE WHEN isPositive THEN 1 ELSE 0 END) as upvotes, SUM(CASE WHEN isPositive THEN 0 ELSE 1 END) as downvotes FROM model_ratings GROUP BY providerId, modelName")
    fun getRatingStats(): Flow<List<ModelRatingStat>>
}

data class ModelRatingStat(
    val modelName: String,
    val providerId: String,
    val upvotes: Int,
    val downvotes: Int
)
