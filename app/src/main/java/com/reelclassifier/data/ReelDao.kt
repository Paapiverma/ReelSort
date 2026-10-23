package com.reelclassifier.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReelDao {
    @Query("SELECT * FROM reels WHERE category = :category ORDER BY savedAt DESC")
    fun getByCategory(category: String): LiveData<List<Reel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reel: Reel): Long

    @Delete
    suspend fun delete(reel: Reel)

    @Query("UPDATE reels SET thumbnailPath = :path WHERE id = :id")
    suspend fun updateThumbnail(id: Int, path: String)

    @Query("SELECT * FROM reels WHERE thumbnailPath IS NULL")
    suspend fun getReelsWithoutThumbnails(): List<Reel>

    @Query("DELETE FROM reels WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
