package com.devomo.domain.repository

import com.devomo.domain.models.Photo
import com.devomo.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getPhotos(page: Int, perPage: Int): Flow<Resource<List<Photo>>>
}