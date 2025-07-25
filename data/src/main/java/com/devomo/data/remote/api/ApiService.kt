package com.devomo.data.remote.api

import com.devomo.data.remote.model.PexelsApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("curated")
    suspend fun getCuratedPhotos(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("per_page") perPage: Int
    ): Response<PexelsApiResponse>
}