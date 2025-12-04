package com.example.otter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.otter.model.AlbumItem
import com.example.otter.model.FunctionItem
import com.example.otter.model.FunctionType
import com.example.otter.model.PhotoItem
import com.example.otter.util.MediaLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
/**
 * 照片选择事件密封类，用于表示从照片选择屏幕出发的、明确的导航事件
 */
sealed class PhotoSelectionEvent {
    data class ScrollFunctionList(val position: Int) : PhotoSelectionEvent()
    data class ScrollAlbumList(val position: Int) : PhotoSelectionEvent()
    data class NavigateToPhotoEditing(val photoUris: ArrayList<String>, val functionType: String) : PhotoSelectionEvent()
}

/**
 * 照片选择ViewModel，用于处理照片选择屏幕的业务逻辑和状态管理
 */
class PhotoSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _functions = MutableStateFlow<List<FunctionItem>>(emptyList())
    val functions = _functions.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos = _photos.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val selectedPhotos = _selectedPhotos.asStateFlow()

    private val _isBatchEditMode = MutableStateFlow(false)
    val isBatchEditMode = _isBatchEditMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _event = MutableSharedFlow<PhotoSelectionEvent>()
    val event = _event.asSharedFlow()

    private var currentPage = 0
    private var currentAlbum: String? = "全部"

    /**
     * 初始化函数列表，不要求权限。
     * 应在屏幕创建时无条件调用。
     */
    fun initFunctions(selectedFunctionName: String?) {
        val functionList = FunctionType.entries.map { FunctionItem(it) }
        val selectedFunction = functionList.find { it.type.displayName == selectedFunctionName } ?: functionList.first()

        functionList.forEach { it.isSelected = it.type == selectedFunction.type }
        _isBatchEditMode.value = selectedFunction.type == FunctionType.BATCH_EDIT
        _functions.value = functionList
    }

    /**
     * 加载相册和照片，要求存储权限。
     * 应在用户授予权限后调用。
     */
    fun loadMedia() {
        loadAlbums()
        loadInitialPhotos()
    }
    /**
     * 清除所有媒体数据，包括相册和照片。
     * 应在用户撤销权限后调用。
     */
    fun clearMedia() {
        _albums.value = emptyList()
        _photos.value = emptyList()
        _selectedPhotos.value = emptyList()
    }
    /**
     * 处理函数点击事件，更新选中状态和照片选择模式。
     * 应在用户点击函数项时调用。
     */
    fun onFunctionClick(position: Int) {
        val clickedFunction = _functions.value[position].type
        _isBatchEditMode.value = clickedFunction == FunctionType.BATCH_EDIT

        if (!_isBatchEditMode.value) {
            _selectedPhotos.value = emptyList()
            val updatedPhotos = _photos.value.map { it.copy(isSelected = false) }
            _photos.value = updatedPhotos
        }

        val currentFunctions = _functions.value.mapIndexed { index, item ->
            item.copy(isSelected = index == position)
        }
        _functions.value = currentFunctions
        viewModelScope.launch {
            _event.emit(PhotoSelectionEvent.ScrollFunctionList(position))
        }
    }

    /**
     * 处理照片点击事件，更新选中状态和导航事件。
     * 应在用户点击照片项时调用。
     */
    fun onPhotoClick(clickedPhoto: PhotoItem) {
        if (_isBatchEditMode.value) {
            val currentSelected = _selectedPhotos.value.toMutableList()
            val existingPhoto = currentSelected.find { it.uri == clickedPhoto.uri }

            if (existingPhoto != null) {
                currentSelected.remove(existingPhoto)
                updatePhotoSelectionState(clickedPhoto, false)
            } else if (currentSelected.size < 9) {
                currentSelected.add(clickedPhoto.copy(isSelected = true))
                updatePhotoSelectionState(clickedPhoto, true)
            }
            _selectedPhotos.value = currentSelected
        } else {
            viewModelScope.launch {
                val functionType = _functions.value.first { it.isSelected }.type.name
                _event.emit(PhotoSelectionEvent.NavigateToPhotoEditing(arrayListOf(clickedPhoto.uri.toString()), functionType))
            }
        }
    }
    /**
     * 处理完成点击事件，导航到照片编辑界面。
     * 应在用户点击完成按钮时调用。
     */
    fun onDoneClick() {
        viewModelScope.launch {
            val photoUris = ArrayList(_selectedPhotos.value.map { it.uri.toString() })
            if (photoUris.isNotEmpty()) {
                val functionType = _functions.value.first { it.isSelected }.type.name
                _event.emit(PhotoSelectionEvent.NavigateToPhotoEditing(photoUris, functionType))
            }
        }
    }
    /**
     * 更新照片选中状态，用于批量编辑模式。
     * 应在用户点击照片项时调用。
     */
    private fun updatePhotoSelectionState(photoItem: PhotoItem, isSelected: Boolean) {
        val updatedPhotos = _photos.value.map {
            if (it.uri == photoItem.uri) {
                it.copy(isSelected = isSelected)
            } else {
                it
            }
        }
        _photos.value = updatedPhotos
    }
    /**
     * 加载相册列表，要求存储权限。
     * 应在用户授予权限后调用。
     */
    private fun loadAlbums() {
        viewModelScope.launch {
            _albums.value = MediaLoader.loadAlbums(getApplication())
        }
    }

    /**
     * 处理相册点击事件，更新选中状态和加载照片。
     * 应在用户点击相册项时调用。
     */
    fun onAlbumClick(position: Int) {
        val selectedAlbumName = _albums.value.getOrNull(position)?.name ?: return
        currentAlbum = selectedAlbumName

        val updatedAlbums = _albums.value.mapIndexed { index, item ->
            item.copy(isSelected = index == position)
        }
        _albums.value = updatedAlbums

        loadInitialPhotos()
        viewModelScope.launch {
            _event.emit(PhotoSelectionEvent.ScrollAlbumList(position))
        }
    }
    /**
     * 处理滚动到列表底部事件，加载更多照片。
     * 应在用户滚动到列表底部时调用。
     */
    fun onScrolledToEnd() {
        loadMorePhotos()
    }
    /**
     * 加载初始照片，用于显示相册中的照片。
     * 应在用户选择相册后调用。
     */
    private fun loadInitialPhotos() {
        currentPage = 0
        _photos.value = emptyList()
        loadPhotos(currentPage)
    }

    /**
     * 加载更多照片，用于分页显示。
     * 应在用户滚动到列表底部时调用。
     */
    private fun loadMorePhotos() {
        if (_isLoading.value) return
        currentPage++
        loadPhotos(currentPage)
    }
    /**
     * 加载指定页面的照片，用于分页显示。
     * 应在用户滚动到列表底部时调用。
     */
    private fun loadPhotos(page: Int) {
        if (_isLoading.value) return
        _isLoading.value = true

        viewModelScope.launch {
            val newPhotos = MediaLoader.loadPhotos(getApplication(), page, currentAlbum)
            // Ensure selection state is restored when loading
            val selectedUris = _selectedPhotos.value.map { it.uri }
            val photosWithSelection = newPhotos.map { it.copy(isSelected = selectedUris.contains(it.uri)) }

            if (page == 0) {
                _photos.value = photosWithSelection
            } else {
                _photos.value = _photos.value + photosWithSelection
            }
            _isLoading.value = false
        }
    }
}