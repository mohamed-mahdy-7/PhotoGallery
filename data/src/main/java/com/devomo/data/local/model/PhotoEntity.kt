package com.devomo.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: Int,
    val imageUrl: String, // Store the 'large' or 'medium' URL from PhotoSrcDto
    val photographer: String,
    val photographerUrl: String,
    val avgColor: String
)