package com.devomo.domain.usecases

import com.devomo.domain.models.Photo
import com.devomo.domain.repository.PhotoRepository
import com.devomo.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    operator fun invoke(page: Int = 1, perPage: Int = 15): Flow<Resource<List<Photo>>> {
        return repository.getPhotos(page, perPage)
    }
}