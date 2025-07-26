package com.devomo.data


import com.devomo.data.local.dao.PhotoDao
import com.devomo.data.local.model.PhotoEntity
import com.devomo.data.remote.api.ApiService
import com.devomo.data.remote.model.PexelsApiResponse
import com.devomo.data.remote.model.PhotoDto
import com.devomo.data.remote.model.PhotoSrcDto
import com.devomo.data.repository.PhotoRepositoryImpl
import com.devomo.domain.models.Photo
import com.devomo.domain.utils.NetworkMonitor
import com.devomo.domain.utils.Resource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.io.IOException

class PhotoRepositoryImplTest {

    private lateinit var repository: PhotoRepositoryImpl
    private val mockApiService: ApiService = mock()
    private val mockPhotoDao: PhotoDao = mock()
    private val mockNetworkMonitor: NetworkMonitor = mock()

    private val testPhotoDtos = listOf(
        PhotoDto(
            id = 1,
            width = 1000,           // قيمة وهمية
            height = 1500,          // قيمة وهمية
            url = "https://example.com/photo1", // قيمة وهمية
            photographer = "Photographer1",
            photographerUrl = "url1",
            photographerId = 101L,  // قيمة وهمية
            avgColor = "#FFF",
            src = PhotoSrcDto("o1", "p1", "l1", "m1", "s1", "t1", "lx1", "l2x1"),
            liked = false,          // قيمة وهمية
            alt = "Test Alt Text 1"
        ),
        PhotoDto(
            id = 2,
            width = 1200,           // قيمة وهمية
            height = 1800,          // قيمة وهمية
            url = "https://example.com/photo2", // قيمة وهمية
            photographer = "Photographer2",
            photographerUrl = "url2",
            avgColor = "#000",
            photographerId = 102L,  // قيمة وهمية
            src = PhotoSrcDto("o2", "p2", "l2", "m2", "s2", "t2", "lx2", "l2x2"),
            liked = true,           // قيمة وهمية
            alt = "Test Alt Text 2"
        )
    )

    // افتراض أن PhotoEntity يحتوي على 'alt'
    private val testPhotoEntities = listOf(
        PhotoEntity(1, "l2x1", "Photographer1", "url1", "#FFF"),
        PhotoEntity(2, "l2x2", "Photographer2", "url2", "#000")
    )

    // افتراض أن Photo (Domain Model) يحتوي على 'alt'
    private val testDomainPhotos = listOf(
        Photo(1, "l2x1", "Photographer1", "url1", "#FFF"),
        Photo(2, "l2x2", "Photographer2", "url2", "#000")
    )

    @Before
    fun setup() {
        repository = PhotoRepositoryImpl(mockApiService, mockPhotoDao, mockNetworkMonitor)
    }

    @Test
    fun `getPhotos emits loading then success when online and API call succeeds`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
        whenever(mockApiService.getCuratedPhotos(any(), any()))
            // **تم تعديل هذا السطر:**
            .thenReturn(
                Response.success(
                    PexelsApiResponse(
                        page = 1,          // افتراض وجود هذا الـ parameter من نوع Int
                        perPage = 15,      // افتراض وجود هذا الـ parameter من نوع Int
                        photos = testPhotoDtos, // الآن الـ photos هي parameter ثالث
                        totalResults = 100,
                        nextPage = "0"
                    )
                )
            )
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(emptyList())
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Success::class.java)
        assertThat((results[1] as Resource.Success).data).isEqualTo(testDomainPhotos)
        verify(mockPhotoDao).deleteAllPhotos()
        verify(mockPhotoDao).insertPhotos(testPhotoEntities)
    }

    @Test
    fun `getPhotos emits loading then success for subsequent page and clears cache`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
        whenever(mockApiService.getCuratedPhotos(2, 15))
            // **تم تعديل هذا السطر هنا أيضاً:**
            .thenReturn(
                Response.success(
                    PexelsApiResponse(
                        page = 2,          // للصفحة الثانية
                        perPage = 15,
                        photos = testPhotoDtos,
                        totalResults = 100,
                        nextPage = "2"
                    )
                )
            )
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(
            listOf(
                PhotoEntity(3, "old_url", "Old Photographer", "old_purl", "#AAA")
            )
        )
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(2, 15).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Success::class.java)
        assertThat((results[1] as Resource.Success).data).isEqualTo(testDomainPhotos)
        verify(mockPhotoDao).deleteAllPhotos()
        verify(mockPhotoDao).insertPhotos(testPhotoEntities)
        verify(mockApiService).getCuratedPhotos(2, 15)
    }

    @Test
    fun `getPhotos emits error when online and API call fails with HTTP error`() = runTest {
        val errorBody =
            "{\"message\":\"Unauthorized\"}".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
        whenever(mockApiService.getCuratedPhotos(any(), any()))
            .thenReturn(Response.error(401, errorBody))
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(emptyList()) // لا توجد بيانات مخبأة
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)


        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Error::class.java)
        assertThat((results[1] as Resource.Error).message).contains("401")
        verify(mockPhotoDao, times(0)).deleteAllPhotos() // لا يتم مسح الـ cache عند الخطأ
        verify(mockPhotoDao, times(0)).insertPhotos(any()) // لا يتم الإدخال عند الخطأ
    }

    @Test
    fun `getPhotos emits error then cached data when online and API call fails but cache exists`() =
        runTest {
            val errorBody =
                "{\"message\":\"Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
            whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
            whenever(mockApiService.getCuratedPhotos(any(), any()))
                .thenReturn(Response.error(500, errorBody))
            whenever(mockPhotoDao.getAllPhotos()).thenReturn(testPhotoEntities) // توجد بيانات مخبأة
            whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
            whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

            val results = repository.getPhotos(1, 15).toList()

            assertThat(results).hasSize(2)
            assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
            assertThat(results[1]).isInstanceOf(Resource.Error::class.java)
            assertThat((results[1] as Resource.Error).data).isEqualTo(testDomainPhotos) // يتم إرجاع البيانات المخبأة
            assertThat((results[1] as Resource.Error).message).contains("Server Error")
            assertThat((results[1] as Resource.Error).message).contains("Loaded from cache due to HTTP error.") // تأكيد الرسالة
        }

    @Test
    fun `getPhotos emits error when online and network error occurs`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
        whenever(mockApiService.getCuratedPhotos(any(), any()))
            .thenThrow(IOException("No network"))
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(emptyList()) // لا توجد بيانات مخبأة
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Error::class.java)
        assertThat((results[1] as Resource.Error).message).contains("Network Error")
        assertThat((results[1] as Resource.Error).message).contains("No cached data available.") // تأكيد الرسالة
    }

    @Test
    fun `getPhotos emits success with cached data when offline`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(false) // لا يوجد اتصال بالإنترنت
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(testPhotoEntities) // توجد بيانات مخبأة
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(1) // فقط حالة Success
        assertThat(results[0]).isInstanceOf(Resource.Success::class.java)
        assertThat((results[0] as Resource.Success).data).isEqualTo(testDomainPhotos)
        assertThat((results[0] as Resource.Success).message).isEqualTo("Offline: Loaded from cache.") // تأكيد الرسالة
        verify(mockApiService, times(0)).getCuratedPhotos(any(), any()) // لم يتم استدعاء الـ API
    }

    @Test
    fun `getPhotos emits error when offline and no cached data`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(false) // لا يوجد اتصال بالإنترنت
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(emptyList()) // لا توجد بيانات مخبأة
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(1)
        assertThat(results[0]).isInstanceOf(Resource.Error::class.java)
        assertThat((results[0] as Resource.Error).message).isEqualTo("Offline: No cached data available.") // تأكيد الرسالة
        verify(mockApiService, times(0)).getCuratedPhotos(any(), any()) // لم يتم استدعاء الـ API
    }

    @Test
    fun `getPhotos handles general unexpected error and falls back to cache`() = runTest {
        whenever(mockNetworkMonitor.isOnline()).thenReturn(true)
        whenever(mockApiService.getCuratedPhotos(any(), any()))
            .thenThrow(RuntimeException("Some unexpected issue")) // خطأ عام
        whenever(mockPhotoDao.getAllPhotos()).thenReturn(testPhotoEntities) // توجد بيانات مخبأة
        whenever(mockPhotoDao.insertPhotos(any())).thenReturn(Unit)
        whenever(mockPhotoDao.deleteAllPhotos()).thenReturn(Unit)

        val results = repository.getPhotos(1, 15).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Error::class.java)
        assertThat((results[1] as Resource.Error).message).contains("An unexpected error occurred")
        assertThat((results[1] as Resource.Error).data).isEqualTo(testDomainPhotos) // تم إرجاع البيانات المخبأة
        assertThat((results[1] as Resource.Error).message).contains("Loaded from cache due to unexpected error.") // تأكيد الرسالة
    }
}