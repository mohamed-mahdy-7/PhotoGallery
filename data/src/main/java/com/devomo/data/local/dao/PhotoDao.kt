package com.devomo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devomo.data.local.model.PhotoEntity

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM photos ORDER BY id DESC") // Example order
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()
}