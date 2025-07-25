package com.devomo.data.remote.model

import com.google.gson.annotations.SerializedName

data class PhotoDto(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    @SerializedName("photographer") val photographer: String,
    @SerializedName("photographer_url") val photographerUrl: String,
    @SerializedName("photographer_id") val photographerId: Long,
    @SerializedName("avg_color") val avgColor: String,
    val src: PhotoSrcDto,
    val liked: Boolean,
    val alt: String
)