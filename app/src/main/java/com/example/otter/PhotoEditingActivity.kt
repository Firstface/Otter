package com.example.otter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.otter.adapter.EditingThumbnailAdapter
import com.example.otter.adapter.EditingToolAdapter
import com.example.otter.databinding.ActivityPhotoEditingBinding
import com.example.otter.model.EditingToolType
import com.example.otter.model.FunctionType
import com.example.otter.renderer.PhotoRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.max

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditingBinding
    private lateinit var renderer: PhotoRenderer

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var isCropMode = false
    private val cropToolIndex by lazy { EditingToolType.entries.indexOf(EditingToolType.CROP) }

    // 持有当前 Bitmap 用于计算尺寸和裁剪
    private var currentBitmap: Bitmap? = null

    companion object {
        const val EXTRA_PHOTO_URIS = "EXTRA_PHOTO_URIS"
        const val EXTRA_FUNCTION_TYPE = "EXTRA_FUNCTION_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupGestures()
        setupButtons()

        val photoUriStrings = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS)
        val functionType = intent.getStringExtra(EXTRA_FUNCTION_TYPE)

        if (photoUriStrings.isNullOrEmpty()) {
            finish()
            return
        }

        val photoUris = photoUriStrings.map { it.toUri() }
        loadBitmap(photoUris[0])

        if (functionType == FunctionType.BATCH_EDIT.name) {
            setupBatchEditMode(photoUris)
        }
    }

    private fun loadBitmap(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                withContext(Dispatchers.Main) {
                    renderer.updateBitmap(currentBitmap!!)
                    binding.glSurfaceView.requestRender()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    private fun setupButtons() {
        binding.ivClose.setOnClickListener { finish() }

        binding.tvSave.setOnClickListener {
            saveAndFinish()
        }

        binding.btnConfirmCrop.setOnClickListener {
            performCrop()
        }
    }

    /**
     * 【核心修复】执行裁剪
     * 使用与 Renderer 完全一致的公式计算图片位置，保证“所见即所得”
     */
    private fun performCrop() {
        val bitmap = currentBitmap ?: return

        val viewWidth = binding.glSurfaceView.width.toFloat()
        val viewHeight = binding.glSurfaceView.height.toFloat()

        // 1. 计算 fit center 基础缩放 (与 Renderer 逻辑一致)
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)

        // 2. 计算图片当前在屏幕上的真实尺寸
        val displayedWidth = bitmap.width * baseScale * renderer.scaleFactor
        val displayedHeight = bitmap.height * baseScale * renderer.scaleFactor

        // 3. 计算图片绘制的起始坐标 (Top-Left)
        // Renderer 逻辑: startX = (viewW - drawW) / 2 + transX
        val imageLeft = (viewWidth - displayedWidth) / 2f + renderer.translationX
        val imageTop = (viewHeight - displayedHeight) / 2f + renderer.translationY

        // 4. 构建图片在屏幕上的矩形范围
        val imageBounds = RectF(
            imageLeft,
            imageTop,
            imageLeft + displayedWidth,
            imageTop + displayedHeight
        )

        android.util.Log.d("CropDebug", "Image Bounds (Pixel): $imageBounds")

        // 5. 获取屏幕上的裁剪框 (交集)
        val validCropRectScreen = binding.cropOverlay.getValidCropRect(imageBounds)

        if (validCropRectScreen != null && !validCropRectScreen.isEmpty) {
            // 6. 坐标逆向映射：屏幕像素 -> Bitmap 像素
            val totalScale = baseScale * renderer.scaleFactor

            // 计算裁剪框左上角相对于图片左上角的距离
            val relativeLeft = validCropRectScreen.left - imageBounds.left
            val relativeTop = validCropRectScreen.top - imageBounds.top

            val cropX = (relativeLeft / totalScale).toInt().coerceIn(0, bitmap.width)
            val cropY = (relativeTop / totalScale).toInt().coerceIn(0, bitmap.height)
            val cropW = (validCropRectScreen.width() / totalScale).toInt().coerceAtMost(bitmap.width - cropX)
            val cropH = (validCropRectScreen.height() / totalScale).toInt().coerceAtMost(bitmap.height - cropY)

            if (cropW > 0 && cropH > 0) {
                // 生成新图
                val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

                // 更新显示
                currentBitmap = croppedBitmap
                renderer.updateBitmap(croppedBitmap)
                binding.glSurfaceView.requestRender()

                // 重置边界限制缓存
                clampTranslation()
                toggleCropMode(false)
            } else {
                Toast.makeText(this, "裁剪区域无效", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未选中图片区域", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndFinish() {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(externalCacheDir, "edited_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()

                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.scaleFactor *= detector.scaleFactor
                renderer.scaleFactor = renderer.scaleFactor.coerceIn(0.1f, 10.0f)

                clampTranslation()
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // 【核心修改】现在直接传递像素距离 (Pixels)，不需要除以 View 宽高
                renderer.translationX -= distanceX
                renderer.translationY -= distanceY

                clampTranslation()
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        binding.glSurfaceView.setOnTouchListener { _, event ->
            if (isCropMode) {
                val consumed = binding.cropOverlay.onTouchEvent(event)
                if (!consumed) {
                    scaleGestureDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)
                }
            } else {
                scaleGestureDetector.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    /**
     * 【核心修复】平移边界限制 (像素级)
     * 策略：
     * 1. 如果图比屏大：限制边缘不进屏幕 (Strict Inside-Out)
     * 2. 如果图比屏小：给予半屏宽度的自由度 (Relaxed Clamping)，解决小图动不了的问题
     */
    private fun clampTranslation() {
        val bitmap = currentBitmap ?: return
        val viewW = binding.glSurfaceView.width.toFloat()
        val viewH = binding.glSurfaceView.height.toFloat()

        // 1. 计算当前真实尺寸 (Pixels)
        val widthRatio = viewW / bitmap.width
        val heightRatio = viewH / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)

        val currentImageW = bitmap.width * baseScale * renderer.scaleFactor
        val currentImageH = bitmap.height * baseScale * renderer.scaleFactor

        // 2. 计算 X 轴限制
        // overflowX: 如果 > 0，说明图比屏大，这是“边缘对齐”所需的最小移动距离。
        //            如果 < 0，说明图比屏小，设为 0 (基础位置居中)。
        val overflowX = max(0f, (currentImageW - viewW) / 2f)

        // 统一规则：在基础范围上，再加半屏宽度的自由度
        // 这样刚进来时 (overflow=0)，limit 也是 0.5 * ViewW，绝对能动！
        val limitX = overflowX + (viewW * 0.5f)

        // 3. 计算 Y 轴限制 (同理)
        val overflowY = max(0f, (currentImageH - viewH) / 2f)
        val limitY = overflowY + (viewH * 0.5f)

        // 4. 应用限制 (使用像素单位)
        renderer.translationX = renderer.translationX.coerceIn(-limitX, limitX)
        renderer.translationY = renderer.translationY.coerceIn(-limitY, limitY)
    }

    private fun setupViews() {
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY

        binding.cropOverlay.onDragStarted = {
            binding.btnConfirmCrop.visibility = View.VISIBLE
        }

        val tools = EditingToolType.entries
        val toolAdapter = EditingToolAdapter(tools) { tool ->
            if (tool == EditingToolType.CROP) {
                toggleCropMode(!isCropMode)
            } else {
                if(isCropMode) toggleCropMode(false)
            }
        }
        binding.rvEditingTools.adapter = toolAdapter
    }

    private fun toggleCropMode(enable: Boolean) {
        isCropMode = enable
        val cropViewHolder = binding.rvEditingTools.findViewHolderForAdapterPosition(cropToolIndex) as? EditingToolAdapter.ToolViewHolder

        if (enable) {
            binding.cropOverlay.visibility = View.VISIBLE
            binding.btnConfirmCrop.visibility = View.GONE
            cropViewHolder?.setTextColor(ContextCompat.getColor(this, R.color.neon_green))
            binding.sbParameter.isEnabled = false
            binding.tvParamName.setTextColor(android.graphics.Color.GRAY)
        } else {
            binding.cropOverlay.visibility = View.GONE
            binding.btnConfirmCrop.visibility = View.GONE
            binding.cropOverlay.reset()
            cropViewHolder?.setTextColor(android.graphics.Color.WHITE)
            binding.sbParameter.isEnabled = true
            binding.tvParamName.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun setupBatchEditMode(photoUris: List<Uri>) {
        binding.rvEditingThumbnails.visibility = View.VISIBLE
        val thumbnailAdapter = EditingThumbnailAdapter(photoUris) { uri ->
            loadBitmap(uri)
        }
        binding.rvEditingThumbnails.adapter = thumbnailAdapter
    }
}