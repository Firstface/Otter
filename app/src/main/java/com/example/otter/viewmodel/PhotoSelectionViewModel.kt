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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class PhotoSelectionEvent {
    data class ScrollFunctionList(val position: Int) : PhotoSelectionEvent()
    data class ScrollAlbumList(val position: Int) : PhotoSelectionEvent()
    data class NavigateToPhotoEditing(val photoUris: ArrayList<String>, val functionType: String) : PhotoSelectionEvent()
}

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
     * Initializes the function list, which does not require permissions.
     * This should be called unconditionally when the screen is created.
     */
    fun initFunctions(selectedFunctionName: String?) {
        val functionList = FunctionType.values().map { FunctionItem(it) }
        val selectedFunction = functionList.find { it.type.displayName == selectedFunctionName } ?: functionList.first()

        functionList.forEach { it.isSelected = it.type == selectedFunction.type }
        _isBatchEditMode.value = selectedFunction.type == FunctionType.BATCH_EDIT
        _functions.value = functionList
    }

    /**
     * Loads media (albums and photos) from the MediaStore. Requires storage permissions.
     */
    fun loadMedia() {
        loadAlbums()
        loadInitialPhotos()
    }

    /**
     * Clears all media data from the UI. Called when permissions are revoked.
     */
    fun clearMedia() {
        _albums.value = emptyList()
        _photos.value = emptyList()
        _selectedPhotos.value = emptyList()
    }

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

    fun onDoneClick() {
        viewModelScope.launch {
            val photoUris = ArrayList(_selectedPhotos.value.map { it.uri.toString() })
            if (photoUris.isNotEmpty()) {
                val functionType = _functions.value.first { it.isSelected }.type.name
                _event.emit(PhotoSelectionEvent.NavigateToPhotoEditing(photoUris, functionType))
            }
        }
    }

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

    private fun loadAlbums() {
        viewModelScope.launch {
            _albums.value = MediaLoader.loadAlbums(getApplication())
        }
    }

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

    fun onScrolledToEnd() {
        loadMorePhotos()
    }

    private fun loadInitialPhotos() {
        currentPage = 0
        _photos.value = emptyList()
        loadPhotos(currentPage)
    }

    private fun loadMorePhotos() {
        if (_isLoading.value) return
        currentPage++
        loadPhotos(currentPage)
    }

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