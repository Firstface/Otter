package com.example.otter

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.otter.databinding.ActivityCustomCameraBinding
import com.example.otter.viewmodel.CameraUIEvent
import com.example.otter.viewmodel.CustomCameraViewModel
import kotlinx.coroutines.launch

/**
 * 自定义相机活动，用于拍照和视频录制
 */
class CustomCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomCameraBinding
    private val viewModel: CustomCameraViewModel by viewModels()

    // ImageCapture 实例与相机生命周期绑定，因此保留在 Activity 中
    private var imageCapture: ImageCapture? = null
    /**
     * 活动创建时调用，初始化视图和相机
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setupClickListeners()
        observeViewModel()
    }

    /**
     * 设置点击事件监听器，包括拍照、切换摄像头、返回
     */
    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener { viewModel.takePhoto(imageCapture) }
        binding.btnFlipCamera.setOnClickListener { viewModel.flipCamera() }
        binding.btnBack.setOnClickListener { finish() }
    }
    /**
     * 观察 ViewModel 中的状态变化，包括摄像头选择器和 UI 事件
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听摄像头选择器的变化
                launch {
                    viewModel.cameraSelector.collect { newSelector ->
                        // 每当选择器变化时，重新启动相机以应用更改
                        startCamera(newSelector)
                    }
                }

                // 监听一次性的UI事件
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is CameraUIEvent.PhotoSaved -> handlePhotoSaved(event.uri)
                            is CameraUIEvent.PhotoCaptureError -> Toast.makeText(baseContext, event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

        /**
         * 处理照片保存事件，显示成功消息和缩略图
         */
        private fun handlePhotoSaved(savedUri: Uri?) {
        val msg = "Photo capture succeeded: $savedUri"
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, msg)

        // 加载缩略图
        Glide.with(this@CustomCameraActivity)
            .load(savedUri)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.ivThumbnail)
    }
        /**
         * 启动相机，绑定预览和 ImageCapture 用例
         */
        private fun startCamera(cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
        /**
         * 检查是否已授予所有必要的权限
         */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
        /**
         * 处理权限请求结果，检查是否所有权限都已授予
         */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(viewModel.cameraSelector.value) // 使用 ViewModel 的当前值
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
        /**
         *  companion object 定义了一些常量，包括标签、请求码和必要的权限
         */
    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
