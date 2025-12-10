package com.example.otter

import android.annotation.SuppressLint
import android.app.Activity
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

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditingBinding
    private lateinit var renderer: PhotoRenderer

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var isCropMode = false
    private var isBrushMode = false
    private val cropToolIndex by lazy { EditingToolType.entries.indexOf(EditingToolType.CROP) }
    private val brushToolIndex by lazy { EditingToolType.entries.indexOf(EditingToolType.BRUSH) }

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
                bitmap.recycle()

                withContext(Dispatchers.Main) {
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
        binding.glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    private fun setupButtons() {
        binding.ivClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.tvSave.setOnClickListener { saveAndFinish() }
        binding.btnConfirmCrop.setOnClickListener { performCrop() }
        binding.btnConfirmBrush.setOnClickListener { performBrushMerge() }

        binding.ivRedo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                currentBitmap = undoStack.pop()
                renderer.updateBitmap(currentBitmap!!, false)
                binding.glSurfaceView.requestRender()
            }
        }

        binding.ivUndo.setOnClickListener {
            originalBitmap?.let {
                val newBitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                currentBitmap = newBitmap
                recycleUndoStack()
                renderer.updateBitmap(currentBitmap!!, true)

                renderer.brightness = 0f           // 重置渲染器的亮度值
                binding.sbParameter.progress = 50  // 重置滑动条位置 (50代表0)
                binding.tvParamName.text = "亮度 0" // 重置显示的文本

                binding.glSurfaceView.requestRender()
            }
        }
    }

    private fun performBrushMerge() {
        saveToHistory()
        val bitmap = currentBitmap ?: return
        val drawingPath = binding.drawingOverlay.getDrawingPath()

        val viewWidth = binding.glSurfaceView.width.toFloat()
        val viewHeight = binding.glSurfaceView.height.toFloat()

        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val totalScale = baseScale * renderer.scaleFactor

        val displayedWidth = bitmap.width * totalScale
        val displayedHeight = bitmap.height * totalScale

        val imageLeft = (viewWidth - displayedWidth) / 2f + renderer.translationX
        val imageTop = (viewHeight - displayedHeight) / 2f + renderer.translationY

        val transformation = Matrix()
        transformation.postScale(totalScale, totalScale)
        transformation.postTranslate(imageLeft, imageTop)

        val inverse = Matrix()
        if (transformation.invert(inverse)) {
            val transformedPath = android.graphics.Path(drawingPath)

            transformedPath.transform(inverse)

            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = android.graphics.Color.RED
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 10f / totalScale
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(transformedPath, paint)

            renderer.updateBitmap(bitmap, false)
            binding.glSurfaceView.requestRender()
        }

        toggleBrushMode(false)
    }

    private fun performCrop() {
        saveToHistory()
        val bitmap = currentBitmap ?: return

        val viewWidth = binding.glSurfaceView.width.toFloat()
        val viewHeight = binding.glSurfaceView.height.toFloat()
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val displayedWidth = bitmap.width * baseScale * renderer.scaleFactor
        val displayedHeight = bitmap.height * baseScale * renderer.scaleFactor
        val imageLeft = (viewWidth - displayedWidth) / 2f + renderer.translationX
        val imageTop = (viewHeight - displayedHeight) / 2f + renderer.translationY

        val imageBounds = RectF(imageLeft, imageTop, imageLeft + displayedWidth, imageTop + displayedHeight)

        val validCropRectScreen = binding.cropOverlay.getValidCropRect(imageBounds)

        if (validCropRectScreen != null && !validCropRectScreen.isEmpty) {
            val totalScale = baseScale * renderer.scaleFactor

            val relativeLeft = validCropRectScreen.left - imageBounds.left
            val relativeTop = validCropRectScreen.top - imageBounds.top

            val cropX = (relativeLeft / totalScale).toInt().coerceIn(0, bitmap.width)
            val cropY = (relativeTop / totalScale).toInt().coerceIn(0, bitmap.height)
            val cropW = (validCropRectScreen.width() / totalScale).toInt().coerceAtMost(bitmap.width - cropX)
            val cropH = (validCropRectScreen.height() / totalScale).toInt().coerceAtMost(bitmap.height - cropY)

            if (cropW > 0 && cropH > 0) {
                val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
                currentBitmap = croppedBitmap

                renderer.updateBitmap(croppedBitmap, true)

                val currentBrightnessValue = (binding.sbParameter.progress - 50) / 100f
                renderer.brightness = currentBrightnessValue

                binding.glSurfaceView.requestRender()

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
        val bitmapToSave = currentBitmap ?: return
        Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val finalBitmap = applyBrightness(bitmapToSave, renderer.brightness)

            try {
                val filename = "Otter_${System.currentTimeMillis()}.jpg"

                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Otter")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = applicationContext.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
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
                if (finalBitmap != bitmapToSave) {
                    finalBitmap.recycle()
                }
            }
        }
    }

    private fun applyBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        if (brightness == 0f) return bitmap

        val adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(adjustedBitmap)
        val paint = Paint()

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
                renderer.translationX -= distanceX
                renderer.translationY -= distanceY
                clampTranslation()
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        binding.glSurfaceView.setOnTouchListener { _, event ->
            when {
                isBrushMode -> {
                    binding.drawingOverlay.onTouchEvent(event)
                }
                isCropMode -> {
                    val consumed = binding.cropOverlay.onTouchEvent(event)
                    if (!consumed) {
                        scaleGestureDetector.onTouchEvent(event)
                        gestureDetector.onTouchEvent(event)
                    }
                }
                else -> {
                    scaleGestureDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)
                }
            }
            true
        }
    }

    private fun clampTranslation() {
        val bitmap = currentBitmap ?: return
        val viewW = binding.glSurfaceView.width.toFloat()
        val viewH = binding.glSurfaceView.height.toFloat()

        val widthRatio = viewW / bitmap.width
        val heightRatio = viewH / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)
        val currentImageW = bitmap.width * baseScale * renderer.scaleFactor
        val currentImageH = bitmap.height * baseScale * renderer.scaleFactor

        val overflowX = max(0f, (currentImageW - viewW) / 2f)
        val limitX = overflowX + (viewW * 0.5f)

        val overflowY = max(0f, (currentImageH - viewH) / 2f)
        val limitY = overflowY + (viewH * 0.5f)

        renderer.translationX = renderer.translationX.coerceIn(-limitX, limitX)
        renderer.translationY = renderer.translationY.coerceIn(-limitY, limitY)
    }

    private fun setupViews() {
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }

        binding.cropOverlay.onDragStarted = {
            binding.btnConfirmCrop.visibility = View.VISIBLE
        }

        val tools = EditingToolType.entries
        val toolAdapter = EditingToolAdapter(tools) { tool ->
            when (tool) {
                EditingToolType.CROP -> toggleCropMode(!isCropMode)
                EditingToolType.BRUSH -> toggleBrushMode(!isBrushMode)
                else -> {
                    if (isCropMode) toggleCropMode(false)
                    if (isBrushMode) toggleBrushMode(false)
                }
            }
        }
        binding.rvEditingTools.adapter = toolAdapter

        binding.sbParameter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val brightnessValue = (progress - 50) / 100f
                    renderer.brightness = brightnessValue
                    binding.glSurfaceView.requestRender()
                    binding.tvParamName.text = "亮度 ${if (brightnessValue > 0) "+" else ""}${(brightnessValue * 100).toInt()}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sbParameter.progress = 50
        binding.tvParamName.text = "亮度 0"
    }

    private fun toggleBrushMode(enable: Boolean) {
        isBrushMode = enable
        if (enable) if (isCropMode) toggleCropMode(false)

        val brushViewHolder = binding.rvEditingTools.findViewHolderForAdapterPosition(brushToolIndex) as? EditingToolAdapter.ToolViewHolder

        if (enable) {
            binding.drawingOverlay.visibility = View.VISIBLE
            binding.btnConfirmBrush.visibility = View.VISIBLE
            brushViewHolder?.setTextColor(ContextCompat.getColor(this, R.color.neon_green))
            binding.sbParameter.isEnabled = false
            binding.tvParamName.setTextColor(android.graphics.Color.GRAY)
        } else {
            binding.drawingOverlay.visibility = View.GONE
            binding.btnConfirmBrush.visibility = View.GONE
            binding.drawingOverlay.reset()
            brushViewHolder?.setTextColor(android.graphics.Color.WHITE)
            binding.sbParameter.isEnabled = true
            binding.tvParamName.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun toggleCropMode(enable: Boolean) {
        isCropMode = enable
        if (enable) if (isBrushMode) toggleBrushMode(false)

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
