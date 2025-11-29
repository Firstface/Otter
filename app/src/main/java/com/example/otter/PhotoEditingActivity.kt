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
import java.util.Stack
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
    private var originalBitmap: Bitmap? = null
    private val undoStack = Stack<Bitmap>()


    companion object {
        const val EXTRA_PHOTO_URIS = "EXTRA_PHOTO_URIS"
        const val EXTRA_FUNCTION_TYPE = "EXTRA_FUNCTION_TYPE"
        private const val UNDO_STACK_CAPACITY = 5
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

    override fun onDestroy() {
        super.onDestroy()
        currentBitmap?.recycle()
        originalBitmap?.recycle()
        recycleUndoStack()
    }

    private fun recycleUndoStack() {
        while (undoStack.isNotEmpty()) {
            undoStack.pop().recycle()
        }
    }

    private fun saveToHistory() {
        currentBitmap?.let {
            if (undoStack.size >= UNDO_STACK_CAPACITY) {
                val oldestBitmap = undoStack.removeAt(0)
                oldestBitmap.recycle()
            }
            undoStack.push(it.copy(it.config ?: Bitmap.Config.ARGB_8888, true))
        }
    }

    private fun loadBitmap(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Recycle previous bitmaps before loading new ones
                currentBitmap?.recycle()
                originalBitmap?.recycle()
                recycleUndoStack()

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                bitmap.recycle() // The originally decoded bitmap is no longer needed


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

        // Swapped button logic as per user request.
        // iv_redo (撤销) -> pop from stack.
        binding.ivRedo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                currentBitmap = undoStack.pop()
                renderer.updateBitmap(currentBitmap!!)
                binding.glSurfaceView.requestRender()
            }
        }

        // iv_undo (重置) -> reset to original.
        binding.ivUndo.setOnClickListener {
            originalBitmap?.let {
                val newBitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                currentBitmap = newBitmap
                recycleUndoStack() // All history is invalid now, recycle the bitmaps.
                renderer.updateBitmap(currentBitmap!!)

                // Reset view
                renderer.scaleFactor = 1f
                renderer.translationX = 0f
                renderer.translationY = 0f

                binding.glSurfaceView.requestRender()
            }
        }
    }

    /**
     * 【核心修复】执行裁剪
     * 使用与 Renderer 完全一致的公式计算图片位置，保证“所见即所得”
     */
    private fun performCrop() {
        saveToHistory()
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

        // 提示用户正在保存
        Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val filename = "Otter_${System.currentTimeMillis()}.jpg"

                // 1. 配置图片属性 (保存到系统相册)
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    // Android 10 (Q) 及以上，指定文件夹为 Pictures/Otter
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Otter")
                        put(MediaStore.MediaColumns.IS_PENDING, 1) // 标记为写入中
                    }
                }

                // 2. 插入到系统图库数据库
                val resolver = applicationContext.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { outputUri ->
                    // 3. 写入 Bitmap 数据
                    resolver.openOutputStream(outputUri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }

                    // 4. Android 10+ 写入完成，解除 Pending 状态（让相册可见）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(outputUri, contentValues, null, null)
                    }
                }

                // 5. 成功后返回
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PhotoEditingActivity, "已保存到相册", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PhotoEditingActivity, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
