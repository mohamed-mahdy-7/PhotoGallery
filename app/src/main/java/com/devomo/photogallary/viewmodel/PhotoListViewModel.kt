package com.devomo.photogallary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devomo.domain.models.Photo
import com.devomo.domain.usecases.GetPhotosUseCase
import com.devomo.domain.utils.NetworkMonitor
import com.devomo.domain.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


@HiltViewModel
class PhotoListViewModel @Inject constructor(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _photoListState = MutableStateFlow<PhotoListState>(PhotoListState.Loading)
    val photoListState = _photoListState.asStateFlow()

    private val _isOnline = MutableStateFlow<Boolean>(true)
    val isOnline = _isOnline.asStateFlow()

    private var currentPage = 1
    private val photosPerPage = 15 // أو أي عدد مناسب لصفحتك

    var isLoadingMore = false // لضمان عدم استدعاء التحميل أكثر من مرة
    var isLastPage = false   // لضمان عدم محاولة تحميل صفحات غير موجودة

    // لكي نحتفظ بالصور الحالية ونضيف عليها الجديدة
    private val _currentPhotos = mutableListOf<Photo>()

    init {
        getPhotos() // تحميل الصفحة الأولى عند التهيئة
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        networkMonitor.isOnlineFlow.onEach { status ->
            _isOnline.value = status
            // إذا عاد الاتصال بالإنترنت، قد ترغب في محاولة إعادة تحميل البيانات
            // إذا كنت في حالة خطأ أو لم يتم تحميل أي شيء بعد
            if (status && _photoListState.value is PhotoListState.Error) {
                // يمكنك إضافة منطق لإعادة التحميل هنا أو فقط ترك المستخدم يسحب للتحديث
                // أو تحميل الصفحة التالية إذا لم يتم التحميل بالكامل
            }
        }.launchIn(viewModelScope)
    }

    fun getPhotos(page: Int = currentPage) {
        if (isLoadingMore || isLastPage) return // لا تفعل شيئًا إذا كنا نحمل بالفعل أو وصلنا للنهاية

        isLoadingMore = true // تعيين علامة التحميل

        // إذا كانت هذه ليست الصفحة الأولى، اعرض حالة تحميل جزئية أو مؤشر تحميل في الأسفل
        if (page == 1) {
            _photoListState.value = PhotoListState.Loading
        } else {
            // يمكنك تعيين حالة تحميل محددة للـ pagination هنا إذا أردت
            // على سبيل المثال: PhotoListState.LoadingMore(currentPhotos)
        }

        viewModelScope.launch {
            getPhotosUseCase(page, photosPerPage).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        val newPhotos = result.data ?: emptyList()
                        if (newPhotos.isEmpty() && page > 1) { // إذا لم يتم جلب صور جديدة بعد الصفحة الأولى
                            isLastPage = true // وصلنا إلى نهاية الصفحات
                        } else {
                            _currentPhotos.addAll(newPhotos) // إضافة الصور الجديدة للقائمة الحالية
                            _photoListState.value =
                                PhotoListState.Success(_currentPhotos.toList()) // تحديث الـ UI
                            currentPage = page // تحديث رقم الصفحة فقط إذا نجح التحميل
                        }
                        isLoadingMore = false // انتهينا من التحميل
                    }

                    is Resource.Error -> {
                        isLoadingMore = false // انتهينا من التحميل (بخطأ)
                        _photoListState.value =
                            PhotoListState.Error(result.message ?: "An unexpected error occurred.")
                        // إذا كانت هناك صور تم تحميلها بالفعل، قد لا ترغب في مسحها بسبب خطأ في الصفحة التالية
                        // لذا يمكنك فقط إظهار رسالة الخطأ للمستخدم
                    }

                    is Resource.Loading -> {
                        // حالة التحميل الأولية تم التعامل معها في بداية الدالة
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    // الدالة التي سيتم استدعاؤها من OnScrollListener
    fun loadNextPage() {
        getPhotos(currentPage + 1)
    }
}

// Sealed class for UI state
sealed class PhotoListState {
    object Loading : PhotoListState() // تحميل الصفحة الأولى

    // data class LoadingMore(val currentPhotos: List<Photo>) : PhotoListState() // ممكن تضيفها لو عايز مؤشر تحميل في الأسفل
    data class Success(val photos: List<Photo>) : PhotoListState()
    data class Error(val message: String) : PhotoListState()
}