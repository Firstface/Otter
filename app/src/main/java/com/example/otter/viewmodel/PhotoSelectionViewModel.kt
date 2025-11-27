package com.example.otter.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.otter.model.AlbumItem
import com.example.otter.model.FunctionItem
import com.example.otter.model.FunctionType
import com.example.otter.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PhotoSelectionEvent {
    data class ScrollFunctionList(val position: Int) : PhotoSelectionEvent()
    data class ScrollAlbumList(val position: Int) : PhotoSelectionEvent()
}

class PhotoSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver = application.contentResolver

    private val _functions = MutableStateFlow<List<FunctionItem>>(emptyList())
    val functions = _functions.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _event = MutableSharedFlow<PhotoSelectionEvent>()
    val event = _event.asSharedFlow()

    private var currentAlbum: String? = null
    private val pageSize = 100

    fun initialize(selectedFunctionName: String?) {
        setupFunctions(selectedFunctionName)
        loadAlbums()
        loadInitialPhotos()
    }

    private fun setupFunctions(selectedFunctionName: String?) {
        val functionList = FunctionType.values().map { FunctionItem(it) }
        functionList.forEach { item ->
            if (item.type.displayName == selectedFunctionName) {
                item.isSelected = true
            }
        }
        _functions.value = functionList
        // ** FIX: Removed event emission from here to prevent race condition **
    }

    fun onFunctionClick(position: Int) {
        val currentFunctions = _functions.value.mapIndexed { index, item ->
            item.copy(isSelected = index == position)
        }
        _functions.value = currentFunctions
        viewModelScope.launch {
            // Event for subsequent clicks is still needed
            _event.emit(PhotoSelectionEvent.ScrollFunctionList(position))
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            val albumSet = mutableSetOf<String>()
            val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    cursor.getString(bucketColumn)?.let { albumSet.add(it) }
                }
            }

            val albumList = mutableListOf(AlbumItem("全部", true))
            albumList.addAll(albumSet.map { AlbumItem(it) })
            _albums.value = albumList
        }
    }

    fun onAlbumClick(position: Int) {
        val selectedAlbumName = _albums.value.getOrNull(position)?.name ?: return
        currentAlbum = if (selectedAlbumName == "全部") null else selectedAlbumName

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
        loadPhotos(0)
    }

    private fun loadMorePhotos() {
        if (_isLoading.value) return
        loadPhotos(_photos.value.size)
    }

    private fun loadPhotos(offset: Int) {
        if (offset > 0 && _isLoading.value) return
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val photoList = mutableListOf<PhotoItem>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val queryArgs = Bundle().apply {
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_TAKEN))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, 1)
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                if (currentAlbum != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(currentAlbum))
                }
            }

            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, queryArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photoList.add(PhotoItem(contentUri))
                }
            }

            if (offset == 0) {
                _photos.value = photoList
            } else {
                _photos.value = _photos.value + photoList
            }
            _isLoading.value = false
        }
    }
}