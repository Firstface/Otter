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

/**
 * 相机界面事件密封类，用于表示相机操作的各种可能结果
 */
sealed class CameraUIEvent {
    /**
     * 照片保存成功事件
     * @param uri 已保存照片的URI地址，可能为null如果保存失败
     */
    data class PhotoSaved(val uri: Uri?) : CameraUIEvent()

    /**
     * 照片捕获失败事件
     * @param message 包含错误信息的描述字符串
     */
    data class PhotoCaptureError(val message: String) : CameraUIEvent()
}

/**
 * 自定义相机视图模型，用于处理相机操作和事件
 */
class CustomCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _event = MutableSharedFlow<CameraUIEvent>()
    val event = _event.asSharedFlow()

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    /**
     * 切换相机，从后摄切换到前摄或从前摄切换到后摄
     */
    fun flipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    /**
     * 拍照，使用提供的ImageCapture实例进行拍照操作
     * @param imageCapture ImageCapture实例，用于拍照操作
     */
    fun takePhoto(imageCapture: ImageCapture?) {
        val imageCaptureInstance = imageCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OtterApp")
            }
        }

        /**
         * 拍照操作，使用提供的ImageCapture实例进行拍照操作
         */
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(getApplication<Application>().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        /**
         * 拍照操作，使用提供的ImageCapture实例进行拍照操作
         */
        imageCaptureInstance.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(getApplication()),
            object : ImageCapture.OnImageSavedCallback {
                /**
                 * 照片捕获失败回调，处理拍照操作失败的情况
                 * @param exc 包含错误信息的ImageCaptureException异常对象
                 */
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exc.message}"
                    Log.e("CameraXApp", msg, exc)
                    viewModelScope.launch {
                        _event.emit(CameraUIEvent.PhotoCaptureError(msg))
                    }
                }
                /**
                 * 照片保存成功回调，处理拍照操作成功的情况
                 * @param output 包含保存照片信息的ImageCapture.OutputFileResults对象
                 */
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        _event.emit(CameraUIEvent.PhotoSaved(output.savedUri))
                    }
                }
            }
        )
    }
    /**
     * 重置相机视图模型，释放相机资源和执行器
     */
    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}