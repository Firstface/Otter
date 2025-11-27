package com.example.otter.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed class CameraUIEvent {
    data class PhotoSaved(val uri: Uri?) : CameraUIEvent()
    data class PhotoCaptureError(val message: String) : CameraUIEvent()
}

class CustomCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _event = MutableSharedFlow<CameraUIEvent>()
    val event = _event.asSharedFlow()

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun flipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun takePhoto(imageCapture: ImageCapture?) {
        val imageCaptureInstance = imageCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OtterApp")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(getApplication<Application>().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        imageCaptureInstance.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(getApplication()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exc.message}"
                    Log.e("CameraXApp", msg, exc)
                    viewModelScope.launch {
                        _event.emit(CameraUIEvent.PhotoCaptureError(msg))
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        _event.emit(CameraUIEvent.PhotoSaved(output.savedUri))
                    }
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}