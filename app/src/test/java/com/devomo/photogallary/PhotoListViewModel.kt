package com.devomo.photogallary


import com.devomo.domain.models.Photo
import com.devomo.domain.usecases.GetPhotosUseCase
import com.devomo.domain.utils.NetworkMonitor
import com.devomo.domain.utils.Resource
import com.devomo.photogallary.viewmodel.PhotoListState
import com.devomo.photogallary.viewmodel.PhotoListViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi // للإشارة إلى استخدام ميزات تجريبية لـ coroutines test
class PhotoListViewModelTest {

    // Mock dependencies
    private val mockGetPhotosUseCase: GetPhotosUseCase = mock()
    private val mockNetworkMonitor: NetworkMonitor = mock()

    // Test dispatcher for controlling coroutine execution
    private val testDispatcher = StandardTestDispatcher()

    // ViewModel under test
    private lateinit var viewModel: PhotoListViewModel

    // Helper data
    private val testPhotos = listOf(
        Photo(1, "url1", "Photographer1", "purl1", "#FFF"),
        Photo(2, "url2", "Photographer2", "purl2", "#000")
    )

    @Before
    fun setup() {
        // Set the main dispatcher to testDispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Default mock behavior for network monitor
        whenever(mockNetworkMonitor.isOnlineFlow).thenReturn(flowOf(true))

        // Initialize ViewModel (this will trigger getPhotos() in init block)
        viewModel = PhotoListViewModel(mockGetPhotosUseCase, mockNetworkMonitor)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher after each test
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load sets loading then success state`() = runTest {
        // Given: UseCase returns success
        whenever(mockGetPhotosUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Resource.Success(testPhotos)))

        // When: ViewModel is initialized (handled in @Before)
        // Advance time to allow coroutines to complete
        advanceUntilIdle()

        // Then: Assert states in order
        val states = mutableListOf<PhotoListState>()
        viewModel.photoListState.collect { states.add(it) } // Collect states

        // Advance to collect the latest state after the init block finishes
        advanceUntilIdle()

        // Verify initial state is Loading, then Success
        assertThat(states[0]).isInstanceOf(PhotoListState.Loading::class.java)
        assertThat(states[1]).isInstanceOf(PhotoListState.Success::class.java)
        assertThat((states[1] as PhotoListState.Success).photos).isEqualTo(testPhotos)
        assertThat(viewModel.isLoadingMore).isFalse() // Should be false after loading
    }

    @Test
    fun `initial load sets loading then error state`() = runTest {
        // Given: UseCase returns error
        whenever(mockGetPhotosUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Resource.Error("Test Error")))

        // When: ViewModel is initialized
        advanceUntilIdle()

        // Then: Assert states
        val states = mutableListOf<PhotoListState>()
        viewModel.photoListState.collect { states.add(it) }
        advanceUntilIdle()

        assertThat(states[0]).isInstanceOf(PhotoListState.Loading::class.java)
        assertThat(states[1]).isInstanceOf(PhotoListState.Error::class.java)
        assertThat((states[1] as PhotoListState.Error).message).isEqualTo("Test Error")
        assertThat(viewModel.isLoadingMore).isFalse()
    }

    @Test
    fun `loadNextPage loads next set of photos`() = runTest {
        // Given: initial load successful, then next load successful
        val initialPhotos = listOf(testPhotos[0])
        val nextPagePhotos = listOf(testPhotos[1])

        whenever(mockGetPhotosUseCase.invoke(1, 15)) // Assuming initial page 1
            .thenReturn(flowOf(Resource.Success(initialPhotos)))
        whenever(mockGetPhotosUseCase.invoke(2, 15)) // Assuming next page 2
            .thenReturn(flowOf(Resource.Success(nextPagePhotos)))

        // Initial setup in @Before will trigger page 1 load
        advanceUntilIdle() // Finish initial load

        assertThat((viewModel.photoListState.value as PhotoListState.Success).photos).isEqualTo(
            initialPhotos
        )
        assertThat(viewModel.isLoadingMore).isFalse()

        // When: Load next page
        viewModel.loadNextPage()
        advanceUntilIdle() // Finish next page load

        // Then: Verify that the use case was called twice and photos are appended
        verify(mockGetPhotosUseCase, times(2)).invoke(any(), any()) // Called for page 1 and page 2

        val finalState = viewModel.photoListState.value
        assertThat(finalState).isInstanceOf(PhotoListState.Success::class.java)
        assertThat((finalState as PhotoListState.Success).photos).isEqualTo(initialPhotos + nextPagePhotos)
        assertThat(viewModel.isLoadingMore).isFalse()
        assertThat(viewModel.isLastPage).isFalse()
    }

    @Test
    fun `loadNextPage sets isLastPage to true when no more photos`() = runTest {
        // Given: Initial load successful, next page returns empty list
        whenever(mockGetPhotosUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Resource.Success(testPhotos)), flowOf(Resource.Success(emptyList())))

        advanceUntilIdle() // Finish initial load

        // When: Load next page
        viewModel.loadNextPage()
        advanceUntilIdle() // Finish next page load

        // Then: isLastPage should be true
        assertThat(viewModel.isLastPage).isTrue()
        assertThat(viewModel.isLoadingMore).isFalse()
    }

    @Test
    fun `network status change updates isOnline flow`() = runTest {
        // Given: Network monitor initially offline, then online
        whenever(mockNetworkMonitor.isOnlineFlow)
            .thenReturn(flowOf(false, true)) // Sequence of network states

        // Re-initialize ViewModel to pick up the new flow sequence
        viewModel = PhotoListViewModel(mockGetPhotosUseCase, mockNetworkMonitor)
        advanceUntilIdle()

        // Verify the sequence of isOnline updates
        val onlineStates = mutableListOf<Boolean>()
        viewModel.isOnline.collect { onlineStates.add(it) }
        advanceUntilIdle()

        assertThat(onlineStates).isEqualTo(listOf(false, true)) // Should reflect the sequence
    }
}