package com.example.otter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.SeekBar
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

/**
 * 照片编辑活动 (PhotoEditingActivity)
 *
 * 核心功能：
 * 1. 图片加载与显示：使用 GLSurfaceView + OpenGL ES 渲染图片，支持高性能的平移和缩放。
 * 2. 图像处理：支持亮度调节 (ColorMatrix)、裁剪 (Crop)、涂鸦 (Brush)。
 * 3. 历史记录：实现了撤销 (Undo) 和重做 (Redo) 功能，基于 Stack 管理 Bitmap 状态。
 * 4. 文件保存：将最终结果合成并保存到系统相册。
 */
class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditingBinding

    // 自定义的 OpenGL 渲染器，负责底层的纹理绘制
    private lateinit var renderer: PhotoRenderer

    // 手势检测器
    private lateinit var scaleGestureDetector: ScaleGestureDetector // 用于双指缩放
    private lateinit var gestureDetector: GestureDetector           // 用于单指拖拽/平移

    // 编辑模式状态标志
    private var isCropMode = false
    private var isBrushMode = false

    // 工具栏索引缓存，用于快速查找 View 更新 UI 状态
    private val cropToolIndex by lazy { EditingToolType.entries.indexOf(EditingToolType.CROP) }
    private val brushToolIndex by lazy { EditingToolType.entries.indexOf(EditingToolType.BRUSH) }

    // 图片数据管理
    private var currentBitmap: Bitmap? = null   // 当前正在显示的图片对象
    private var originalBitmap: Bitmap? = null  // 原始加载的图片（用于重置或作为基准）

    // 撤销栈：保存之前的 Bitmap 状态，用于 "撤销" 操作
    private val undoStack = Stack<Bitmap>()

    companion object {
        const val EXTRA_PHOTO_URIS = "EXTRA_PHOTO_URIS"     // Intent 传递图片 URI 列表的 Key
        const val EXTRA_FUNCTION_TYPE = "EXTRA_FUNCTION_TYPE" // Intent 传递功能类型的 Key
        private const val UNDO_STACK_CAPACITY = 5           // 最大撤销步数，防止内存溢出
    }

    /**
     * 生命周期：创建
     * 初始化 UI、手势监听器，并解析 Intent 数据加载图片
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupGestures()
        setupButtons()

        // 获取传递过来的图片 URI 列表
        val photoUriStrings = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS)
        val functionType = intent.getStringExtra(EXTRA_FUNCTION_TYPE)

        if (photoUriStrings.isNullOrEmpty()) {
            finish()
            return
        }

        // 默认加载第一张图片
        val photoUris = photoUriStrings.map { it.toUri() }
        loadBitmap(photoUris[0])

        // 如果是批量编辑模式，显示底部缩略图列表
        if (functionType == FunctionType.BATCH_EDIT.name) {
            setupBatchEditMode(photoUris)
        }
    }

    /**
     * 生命周期：销毁
     * 关键点：必须手动回收 Bitmap 内存，防止内存泄漏 (OOM)
     */
    override fun onDestroy() {
        super.onDestroy()
        currentBitmap?.recycle()
        originalBitmap?.recycle()
        recycleUndoStack()
    }

    /**
     * 清空并回收撤销栈中的所有 Bitmap
     */
    private fun recycleUndoStack() {
        while (undoStack.isNotEmpty()) {
            undoStack.pop().recycle()
        }
    }

    /**
     * 保存当前状态到历史记录 (Undo Stack)
     * 在进行破坏性编辑（如裁剪、涂鸦结束）前调用
     */
    private fun saveToHistory() {
        currentBitmap?.let {
            // 如果栈已满，移除最旧的一个并回收内存
            if (undoStack.size >= UNDO_STACK_CAPACITY) {
                val oldestBitmap = undoStack.removeAt(0)
                oldestBitmap.recycle()
            }
            // 压入当前 Bitmap 的深拷贝 (Deep Copy)
            undoStack.push(it.copy(it.config ?: Bitmap.Config.ARGB_8888, true))
        }
    }

    /**
     * 异步加载图片
     * 根据 Android 版本选择 ImageDecoder (API 28+) 或 MediaStore
     */
    private fun loadBitmap(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 加载新图前，清理旧图内存
                currentBitmap?.recycle()
                originalBitmap?.recycle()
                recycleUndoStack()

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        // 必须设置为可变，否则无法进行 Canvas 绘制
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                // 创建副本以确保配置为 ARGB_8888 (高质量) 且可变
                originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                // 原图如果不再需要可以回收（这里因为 copy 了所以回收原始的解码结果）
                if (bitmap != currentBitmap && bitmap != originalBitmap) {
                    bitmap.recycle()
                }

                withContext(Dispatchers.Main) {
                    // 通知渲染器更新纹理，true 表示重置缩放和平移位置
                    renderer.updateBitmap(currentBitmap!!, true)
                    binding.glSurfaceView.requestRender()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause() // 暂停 GL 线程
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume() // 恢复 GL 线程
    }

    /**
     * 绑定按钮点击事件
     */
    private fun setupButtons() {
        binding.ivClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.tvSave.setOnClickListener { saveAndFinish() }

        // 确认裁剪/涂鸦的按钮
        binding.btnConfirmCrop.setOnClickListener { performCrop() }
        binding.btnConfirmBrush.setOnClickListener { performBrushMerge() }

        // 重做 (Redo) - 目前逻辑似乎是当作 "撤销到上一步" ?
        // 注意：代码逻辑中 ivRedo 使用了 pop()，这实际上是 "Undo" 的行为。
        // 如果需要真正的 Redo，通常需要两个栈 (undoStack 和 redoStack)。
        // 这里的命名可能需要根据实际业务逻辑确认，暂时按代码逻辑注释为 "恢复栈顶图片"。
        binding.ivRedo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                currentBitmap = undoStack.pop()
                renderer.updateBitmap(currentBitmap!!, false)
                binding.glSurfaceView.requestRender()
            }
        }

        // 撤销 (Undo) / 重置 (Reset)
        // 当前逻辑看起来像是 "重置回原图 (Reset)"，而不是一步步撤销。
        binding.ivUndo.setOnClickListener {
            originalBitmap?.let {
                val newBitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                currentBitmap = newBitmap
                recycleUndoStack() // 清空历史
                renderer.updateBitmap(currentBitmap!!, true)
                binding.glSurfaceView.requestRender()
            }
        }
    }

    /**
     *  合并涂鸦层到 Bitmap
     * 难点：将屏幕上的触摸路径 (Screen Coordinates) 映射回 Bitmap 的内部像素坐标 (Bitmap Coordinates)。
     */
    private fun performBrushMerge() {
        saveToHistory() // 保存当前状态以便撤销
        val bitmap = currentBitmap ?: return

        // 获取用户在覆盖层(Overlay)上绘制的路径
        val drawingPath = binding.drawingOverlay.getDrawingPath()

        // 1. 获取当前视图尺寸
        val viewWidth = binding.glSurfaceView.width.toFloat()
        val viewHeight = binding.glSurfaceView.height.toFloat()

        // 2. 计算图片当前的显示比例 (Fit Center 基础比例 * 用户手势缩放比例)
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val totalScale = baseScale * renderer.scaleFactor

        // 3. 计算图片在屏幕上的实际显示尺寸
        val displayedWidth = bitmap.width * totalScale
        val displayedHeight = bitmap.height * totalScale

        // 4. 计算图片的左上角在屏幕上的坐标 (居中偏移 + 用户平移偏移)
        val imageLeft = (viewWidth - displayedWidth) / 2f + renderer.translationX
        val imageTop = (viewHeight - displayedHeight) / 2f + renderer.translationY

        // 5. 构建正向变换矩阵：Bitmap -> Screen
        val transformation = Matrix()
        transformation.postScale(totalScale, totalScale) // 缩放
        transformation.postTranslate(imageLeft, imageTop) // 平移

        // 6. 计算逆矩阵：Screen -> Bitmap
        // 我们需要把屏幕上的 Path 转换回 Bitmap 内部坐标系进行绘制
        val inverse = Matrix()
        if (transformation.invert(inverse)) {
            val transformedPath = android.graphics.Path(drawingPath)

            //
            // 应用逆矩阵变换路径
            transformedPath.transform(inverse)

            // 在 Bitmap 上创建 Canvas 进行绘制
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = android.graphics.Color.RED
                isAntiAlias = true
                style = Paint.Style.STROKE
                // 笔触宽度也需要反向缩放，否则放大图片时笔触会变得非常细
                strokeWidth = 10f / totalScale
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(transformedPath, paint)

            // 更新渲染器，false 表示保留当前的缩放和平移位置，不要重置视图
            renderer.updateBitmap(bitmap, false)
            binding.glSurfaceView.requestRender()
        }

        toggleBrushMode(false) // 退出涂鸦模式
    }

    /**
     * 执行裁剪
     * 计算屏幕上的裁剪框对应的 Bitmap 像素区域，并创建新的 Bitmap。
     */
    private fun performCrop() {
        saveToHistory()
        val bitmap = currentBitmap ?: return

        // 1. 基础参数计算 (同上)
        val viewWidth = binding.glSurfaceView.width.toFloat()
        val viewHeight = binding.glSurfaceView.height.toFloat()
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val displayedWidth = bitmap.width * baseScale * renderer.scaleFactor
        val displayedHeight = bitmap.height * baseScale * renderer.scaleFactor
        val imageLeft = (viewWidth - displayedWidth) / 2f + renderer.translationX
        val imageTop = (viewHeight - displayedHeight) / 2f + renderer.translationY

        // 图片在屏幕上的矩形区域
        val imageBounds = RectF(imageLeft, imageTop, imageLeft + displayedWidth, imageTop + displayedHeight)

        // 获取裁剪框在屏幕上的有效区域 (Intersection of CropOverlay and ImageBounds)
        val validCropRectScreen = binding.cropOverlay.getValidCropRect(imageBounds)

        if (validCropRectScreen != null && !validCropRectScreen.isEmpty) {
            val totalScale = baseScale * renderer.scaleFactor

            // 2. 将屏幕坐标转换为 Bitmap 相对坐标
            val relativeLeft = validCropRectScreen.left - imageBounds.left
            val relativeTop = validCropRectScreen.top - imageBounds.top

            // 3. 计算裁剪区域在原图上的 (x, y, w, h)，并处理边界溢出
            val cropX = (relativeLeft / totalScale).toInt().coerceIn(0, bitmap.width)
            val cropY = (relativeTop / totalScale).toInt().coerceIn(0, bitmap.height)
            val cropW = (validCropRectScreen.width() / totalScale).toInt().coerceAtMost(bitmap.width - cropX)
            val cropH = (validCropRectScreen.height() / totalScale).toInt().coerceAtMost(bitmap.height - cropY)

            if (cropW > 0 && cropH > 0) {
                // 执行裁剪，创建新 Bitmap
                val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
                currentBitmap = croppedBitmap

                // 更新渲染器 (true: 重置视图，让裁剪后的图片居中显示)
                renderer.updateBitmap(croppedBitmap, true)
                binding.glSurfaceView.requestRender()

                // 确保平移不越界
                clampTranslation()
                toggleCropMode(false)
            } else {
                Toast.makeText(this, "裁剪区域无效", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未选中图片区域", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存最终图片并结束 Activity
     * 包含亮度滤镜的应用逻辑
     */
    private fun saveAndFinish() {
        val bitmapToSave = currentBitmap ?: return
        Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            // 在保存前，将当前的亮度设置应用到 Bitmap 上（永久固化）
            //
            val finalBitmap = applyBrightness(bitmapToSave, renderer.brightness)

            try {
                val filename = "Otter_${System.currentTimeMillis()}.jpg"

                // 配置 MediaStore 参数
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    // Android Q (10) 及以上支持 Scoped Storage
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Otter")
                        put(MediaStore.MediaColumns.IS_PENDING, 1) // 标记为处理中，其他应用暂不可见
                    }
                }

                val resolver = applicationContext.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    // 写入流
                    resolver.openOutputStream(it)?.use { stream ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                    // Android Q+ 完成写入后，取消 Pending 状态
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }
                }

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
            } finally {
                // 如果应用了滤镜生成了新 Bitmap，需要回收它
                if (finalBitmap != bitmapToSave) {
                    finalBitmap.recycle()
                }
            }
        }
    }

    /**
     * 使用 ColorMatrix 应用亮度调整
     * 返回一个新的处理后的 Bitmap
     */
    private fun applyBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        if (brightness == 0f) return bitmap

        val adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(adjustedBitmap)
        val paint = Paint()

        // 亮度矩阵计算：[R, G, B, A, Offset]
        // Offset 直接加到 RGB 分量上
        val brightnessValue = brightness * 255
        val colorMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightnessValue, // Red
            0f, 1f, 0f, 0f, brightnessValue, // Green
            0f, 0f, 1f, 0f, brightnessValue, // Blue
            0f, 0f, 0f, 1f, 0f               // Alpha
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return adjustedBitmap
    }

    /**
     * 配置手势监听器
     * 处理缩放 (Scale) 和 拖拽 (Scroll)
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        // 1. 缩放手势
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.scaleFactor *= detector.scaleFactor
                // 限制最大最小缩放比例 (0.1x - 10x)
                renderer.scaleFactor = renderer.scaleFactor.coerceIn(0.1f, 10.0f)

                // 缩放后重新检查边界，防止图片完全移出屏幕
                clampTranslation()
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        // 2. 滚动手势 (平移)
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // 注意：OpenGL 坐标系移动方向可能与屏幕手势相反，这里直接减去距离
                renderer.translationX -= distanceX
                renderer.translationY -= distanceY
                clampTranslation()
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        // 3. 触摸事件分发中心
        binding.glSurfaceView.setOnTouchListener { _, event ->
            when {
                // 涂鸦模式：事件交给 DrawingOverlay 处理
                isBrushMode -> {
                    binding.drawingOverlay.onTouchEvent(event)
                }
                // 裁剪模式：事件优先交给 CropOverlay (调整裁剪框)，如果没消耗则交给手势 (缩放/移动图片)
                isCropMode -> {
                    val consumed = binding.cropOverlay.onTouchEvent(event)
                    if (!consumed) {
                        scaleGestureDetector.onTouchEvent(event)
                        gestureDetector.onTouchEvent(event)
                    }
                }
                // 普通模式：处理缩放和平移
                else -> {
                    scaleGestureDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)
                }
            }
            true
        }
    }

    /**
     * 限制渲染器的平移量
     * 确保图片至少有一部分保留在视图中心附近，防止用户将图片完全拖出屏幕找不到了。
     */
    private fun clampTranslation() {
        val bitmap = currentBitmap ?: return
        val viewW = binding.glSurfaceView.width.toFloat()
        val viewH = binding.glSurfaceView.height.toFloat()

        // 计算当前图片的实际尺寸
        val widthRatio = viewW / bitmap.width
        val heightRatio = viewH / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val currentImageW = bitmap.width * baseScale * renderer.scaleFactor
        val currentImageH = bitmap.height * baseScale * renderer.scaleFactor

        // 计算允许的最大溢出量 (图片宽/高 与 视图宽/高 之差的一半)
        val overflowX = max(0f, (currentImageW - viewW) / 2f)
        // 允许额外移动半个屏幕的距离
        val limitX = overflowX + (viewW * 0.5f)

        val overflowY = max(0f, (currentImageH - viewH) / 2f)
        val limitY = overflowY + (viewH * 0.5f)

        // 限制 translationXY 在范围内
        renderer.translationX = renderer.translationX.coerceIn(-limitX, limitX)
        renderer.translationY = renderer.translationY.coerceIn(-limitY, limitY)
    }

    /**
     * 初始化视图组件
     */
    private fun setupViews() {
        // 配置 OpenGL SurfaceView
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.apply {
            setEGLContextClientVersion(2) // 使用 OpenGL ES 2.0
            setRenderer(renderer)
            // 仅在数据变化 (requestRender) 时重绘，节省电量
            renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }

        // 裁剪框开始拖动时，显示确认按钮
        binding.cropOverlay.onDragStarted = {
            binding.btnConfirmCrop.visibility = View.VISIBLE
        }

        // 初始化底部工具栏适配器
        val tools = EditingToolType.entries
        val toolAdapter = EditingToolAdapter(tools) { tool ->
            when (tool) {
                EditingToolType.CROP -> toggleCropMode(!isCropMode)
                EditingToolType.BRUSH -> toggleBrushMode(!isBrushMode)
                else -> {
                    // 切换到其他工具时，关闭特殊模式
                    if (isCropMode) toggleCropMode(false)
                    if (isBrushMode) toggleBrushMode(false)
                }
            }
        }
        binding.rvEditingTools.adapter = toolAdapter

        // 初始化亮度调节 SeekBar
        binding.sbParameter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 将 0-100 映射到 -0.5 到 +0.5 (假设逻辑) 或 0 到 1
                    // 这里的代码逻辑是 (P - 50) / 100f，即 -0.5 到 0.5
                    val brightnessValue = (progress - 50) / 100f
                    renderer.brightness = brightnessValue
                    binding.glSurfaceView.requestRender()
                    binding.tvParamName.text = "亮度 ${if (brightnessValue > 0) "+" else ""}${(brightnessValue * 100).toInt()}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 初始状态
        binding.sbParameter.progress = 50
        binding.tvParamName.text = "亮度 0"
    }

    /**
     * 切换涂鸦模式 UI 状态
     */
    private fun toggleBrushMode(enable: Boolean) {
        isBrushMode = enable
        // 互斥逻辑：如果开启涂鸦，则关闭裁剪
        if (enable) if (isCropMode) toggleCropMode(false)

        val brushViewHolder = binding.rvEditingTools.findViewHolderForAdapterPosition(brushToolIndex) as? EditingToolAdapter.ToolViewHolder

        if (enable) {
            binding.drawingOverlay.visibility = View.VISIBLE
            binding.btnConfirmBrush.visibility = View.VISIBLE
            brushViewHolder?.setTextColor(ContextCompat.getColor(this, R.color.neon_green)) // 高亮图标
            binding.sbParameter.isEnabled = false // 涂鸦时禁用亮度调节
            binding.tvParamName.setTextColor(android.graphics.Color.GRAY)
        } else {
            binding.drawingOverlay.visibility = View.GONE
            binding.btnConfirmBrush.visibility = View.GONE
            binding.drawingOverlay.reset() // 清空画布上的临时路径
            brushViewHolder?.setTextColor(android.graphics.Color.WHITE)
            binding.sbParameter.isEnabled = true
            binding.tvParamName.setTextColor(android.graphics.Color.WHITE)
        }
    }

    /**
     * 切换裁剪模式 UI 状态
     */
    private fun toggleCropMode(enable: Boolean) {
        isCropMode = enable
        if (enable) if (isBrushMode) toggleBrushMode(false)

        val cropViewHolder = binding.rvEditingTools.findViewHolderForAdapterPosition(cropToolIndex) as? EditingToolAdapter.ToolViewHolder

        if (enable) {
            binding.cropOverlay.visibility = View.VISIBLE
            binding.btnConfirmCrop.visibility = View.GONE // 刚进入时不显示确认，拖动后才显示
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

    /**
     * 配置批量编辑模式
     * 显示底部的图片缩略图列表，点击可切换当前编辑图片
     */
    private fun setupBatchEditMode(photoUris: List<Uri>) {
        binding.rvEditingThumbnails.visibility = View.VISIBLE
        val thumbnailAdapter = EditingThumbnailAdapter(photoUris) { uri ->
            loadBitmap(uri)
        }
        binding.rvEditingThumbnails.adapter = thumbnailAdapter
    }
}