package com.devomo.data.di

import com.devomo.data.local.dao.PhotoDao
import com.devomo.data.remote.api.ApiService
import com.devomo.data.repository.PhotoRepositoryImpl
import com.devomo.domain.repository.PhotoRepository
import com.devomo.domain.utils.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePhotoRepository(
        apiService: ApiService,
        photoDao: PhotoDao,
        networkMonitor: NetworkMonitor
    ): PhotoRepository {
        return PhotoRepositoryImpl(apiService, photoDao, networkMonitor)
    }
}