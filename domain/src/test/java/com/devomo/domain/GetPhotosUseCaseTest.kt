package com.devomo.domain

import com.devomo.domain.models.Photo
import com.devomo.domain.repository.PhotoRepository
import com.devomo.domain.usecases.GetPhotosUseCase
import com.devomo.domain.utils.Resource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class GetPhotosUseCaseTest {

    private lateinit var getPhotosUseCase: GetPhotosUseCase
    private val mockPhotoRepository: PhotoRepository = mock()

    private val testPhotos = listOf(
        Photo(1, "url1", "Photographer1", "purl1", "#FFF")
    )

    @Before
    fun setup() {
        getPhotosUseCase = GetPhotosUseCase(mockPhotoRepository)
    }

    @Test
    fun `invoke calls repository with correct parameters and emits success`() = runTest {
        // Given
        val page = 1
        val perPage = 15
        whenever(mockPhotoRepository.getPhotos(page, perPage))
            .thenReturn(flowOf(Resource.Success(testPhotos)))

        // When
        val result = getPhotosUseCase.invoke(page, perPage).first()

        // Then
        verify(mockPhotoRepository).getPhotos(page, perPage)
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data).isEqualTo(testPhotos)
    }

    @Test
    fun `invoke calls repository and emits error`() = runTest {
        // Given
        val errorMessage = "Network error"
        whenever(mockPhotoRepository.getPhotos(any(), any()))
            .thenReturn(flowOf(Resource.Error(errorMessage)))

        // When
        val result = getPhotosUseCase.invoke(1, 15).first()

        // Then
        verify(mockPhotoRepository).getPhotos(1, 15)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).message).isEqualTo(errorMessage)
    }
}