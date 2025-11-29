package com.example.otter

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.otter.adapter.EditingThumbnailAdapter
import com.example.otter.adapter.EditingToolAdapter
import com.example.otter.databinding.ActivityPhotoEditingBinding
import com.example.otter.model.EditingToolType
import com.example.otter.model.FunctionType
import com.example.otter.renderer.PhotoRenderer

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditingBinding
    private lateinit var renderer: PhotoRenderer

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var isCropMode = false
    private val cropToolIndex by lazy { EditingToolType.values().indexOf(EditingToolType.CROP) }

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
        loadPhotoIntoRenderer(photoUris[0])

        if (functionType == FunctionType.BATCH_EDIT.name) {
            setupBatchEditMode(photoUris)
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

        binding.btnConfirmCrop.setOnClickListener {
            val cropRect = binding.cropOverlay.getCropRect()
            if (!cropRect.isEmpty) {
                val message = "TODO: Crop Rect Acquired: [${cropRect.left}, ${cropRect.top}, ${cropRect.right}, ${cropRect.bottom}]"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d("CropInfo", message)
                // TODO: Pass these normalized coordinates to OpenGL for the actual crop
            }
            toggleCropMode(false) // Exit crop mode after confirmation
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.scaleFactor *= detector.scaleFactor
                renderer.scaleFactor = renderer.scaleFactor.coerceIn(0.1f, 10.0f)
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                renderer.translationX -= distanceX / binding.glSurfaceView.width
                renderer.translationY += distanceY / binding.glSurfaceView.height
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        binding.glSurfaceView.setOnTouchListener { _, event ->
            if (isCropMode) {
                binding.cropOverlay.onTouchEvent(event)
            } else {
                scaleGestureDetector.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    private fun setupViews() {
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        binding.cropOverlay.onDragStarted = {
            binding.btnConfirmCrop.visibility = View.VISIBLE
        }

        val tools = EditingToolType.values().toList()
        val toolAdapter = EditingToolAdapter(tools) { tool ->
            if (tool == EditingToolType.CROP) {
                toggleCropMode(!isCropMode) // Toggle crop mode
            } else {
                if(isCropMode) toggleCropMode(false) // Exit crop mode if another tool is selected
                // TODO: Handle other tools
            }
        }
        binding.rvEditingTools.adapter = toolAdapter
    }

    private fun toggleCropMode(enable: Boolean) {
        isCropMode = enable
        val cropViewHolder = binding.rvEditingTools.findViewHolderForAdapterPosition(cropToolIndex) as? EditingToolAdapter.ToolViewHolder

        if (enable) {
            // --- ENTER CROP MODE ---
            binding.cropOverlay.visibility = View.VISIBLE
            binding.btnConfirmCrop.visibility = View.GONE // Initially hidden

            cropViewHolder?.setTextColor(ContextCompat.getColor(this, R.color.neon_green))

            // Disable the slider
            binding.sbParameter.isEnabled = false
            binding.tvParamName.setTextColor(Color.GRAY)

        } else {
            // --- EXIT CROP MODE ---
            binding.cropOverlay.visibility = View.GONE
            binding.btnConfirmCrop.visibility = View.GONE
            binding.cropOverlay.reset()

            cropViewHolder?.setTextColor(Color.WHITE)

            // Re-enable the slider
            binding.sbParameter.isEnabled = true
            binding.tvParamName.setTextColor(Color.WHITE)
        }
    }

    private fun setupBatchEditMode(photoUris: List<Uri>) {
        binding.rvEditingThumbnails.visibility = View.VISIBLE
        val thumbnailAdapter = EditingThumbnailAdapter(photoUris) { uri ->
            loadPhotoIntoRenderer(uri)
        }
        binding.rvEditingThumbnails.adapter = thumbnailAdapter
    }

    private fun loadPhotoIntoRenderer(uri: Uri) {
        if(isCropMode) {
            toggleCropMode(false)
        }
        renderer.setPhotoUri(uri)
        binding.glSurfaceView.requestRender()
    }
}