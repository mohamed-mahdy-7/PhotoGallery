package com.devomo.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devomo.data.local.dao.PhotoDao
import com.devomo.data.local.model.PhotoEntity

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}