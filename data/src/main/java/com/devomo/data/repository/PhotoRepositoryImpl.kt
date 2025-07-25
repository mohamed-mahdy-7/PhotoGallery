package com.devomo.data.repository

import com.devomo.data.local.dao.PhotoDao
import com.devomo.data.remote.api.ApiService
import com.devomo.data.utils.toDomain
import com.devomo.data.utils.toEntity
import com.devomo.domain.models.Photo
import com.devomo.domain.repository.PhotoRepository
import com.devomo.domain.utils.NetworkMonitor
import com.devomo.domain.utils.Resource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val photoDao: PhotoDao,
    private val networkMonitor: NetworkMonitor
) : PhotoRepository {

    override fun getPhotos(page: Int, perPage: Int): Flow<Resource<List<Photo>>> = flow {
        emit(Resource.Loading())

        val isConnected = networkMonitor.isOnline() // Get current network status

        if (isConnected) {
            try {
                val response = apiService.getCuratedPhotos(page, perPage)
                if (response.isSuccessful) {
                    val photoDtos = response.body()?.photos ?: emptyList()
                    val domainPhotos = photoDtos.map { it.toDomain() }
                    val photoEntities = domainPhotos.map { it.toEntity() }

                    // Cache data
                    photoDao.deleteAllPhotos() // Or more sophisticated caching logic
                    photoDao.insertPhotos(photoEntities)

                    emit(Resource.Success(domainPhotos))
                } else {
                    val errorMessage = "API error: ${response.code()} ${response.message()}"
                    emit(Resource.Error(errorMessage))
                    // Try to load from cache on API error if available
                    val cachedPhotos = photoDao.getAllPhotos().map { it.toDomain() }
                    if (cachedPhotos.isNotEmpty()) {
                        emit(Resource.Success(cachedPhotos, "Loaded from cache due to API error."))
                    } else {
                        emit(Resource.Error(errorMessage + " No cached data available."))
                    }
                }
            } catch (e: HttpException) {
                val errorMessage = "HTTP Error: ${e.message()}"
                emit(Resource.Error(errorMessage))
                // Try to load from cache on network error
                val cachedPhotos = photoDao.getAllPhotos().map { it.toDomain() }
                if (cachedPhotos.isNotEmpty()) {
                    emit(Resource.Success(cachedPhotos, "Loaded from cache due to HTTP error."))
                } else {
                    emit(Resource.Error(errorMessage + " No cached data available."))
                }
            } catch (e: IOException) {
                val errorMessage = "Network Error: Check your connection."
                emit(Resource.Error(errorMessage))
                // Always load from cache on IO/network error
                val cachedPhotos = photoDao.getAllPhotos().map { it.toDomain() }
                if (cachedPhotos.isNotEmpty()) {
                    emit(Resource.Success(cachedPhotos, "Loaded from cache as offline."))
                } else {
                    emit(Resource.Error(errorMessage + " No cached data available."))
                }
            } catch (e: Exception) {
                val errorMessage = "An unexpected error occurred: ${e.message}"
                emit(Resource.Error(errorMessage))
                // Fallback to cache for any other error
                val cachedPhotos = photoDao.getAllPhotos().map { it.toDomain() }
                if (cachedPhotos.isNotEmpty()) {
                    emit(
                        Resource.Success(
                            cachedPhotos,
                            "Loaded from cache due to unexpected error."
                        )
                    )
                } else {
                    emit(Resource.Error(errorMessage + " No cached data available."))
                }
            }
        } else { // Offline scenario
            val cachedPhotos = photoDao.getAllPhotos().map { it.toDomain() }
            if (cachedPhotos.isNotEmpty()) {
                emit(Resource.Success(cachedPhotos, "Offline: Loaded from cache."))
            } else {
                emit(Resource.Error("Offline: No cached data available."))
            }
        }
    }
}