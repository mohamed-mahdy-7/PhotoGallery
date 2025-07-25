package com.devomo.domain.models

data class Photo(
    val id: Int,
    val imageUrl: String,
    val photographer: String,
    val photographerUrl: String,
    val avgColor: String
)
