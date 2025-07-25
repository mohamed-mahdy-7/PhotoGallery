package com.devomo.data.remote.model

import com.google.gson.annotations.SerializedName

data class PexelsApiResponse(
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    val photos: List<PhotoDto>,
    @SerializedName("total_results") val totalResults: Int,
    @SerializedName("next_page") val nextPage: String?
)