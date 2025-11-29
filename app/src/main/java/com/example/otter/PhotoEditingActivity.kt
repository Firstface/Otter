package com.example.otter

import android.annotation.SuppressLint
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

    private fun setupButtons(){
        binding.ivClose.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.scaleFactor *= detector.scaleFactor
                renderer.scaleFactor = renderer.scaleFactor.coerceIn(0.1f, 10.0f) // Limit zoom
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // Normalize distances based on view size for consistent panning speed
                renderer.translationX -= distanceX / binding.glSurfaceView.width
                renderer.translationY += distanceY / binding.glSurfaceView.height
                binding.glSurfaceView.requestRender()
                return true
            }
        })

        binding.glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupViews() {
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        val tools = EditingToolType.entries
        val toolAdapter = EditingToolAdapter(tools) { tool ->
            // Handle tool selection
        }
        binding.rvEditingTools.adapter = toolAdapter
    }

    private fun setupBatchEditMode(photoUris: List<Uri>) {
        binding.rvEditingThumbnails.visibility = View.VISIBLE
        val thumbnailAdapter = EditingThumbnailAdapter(photoUris) { uri ->
            loadPhotoIntoRenderer(uri)
        }
        binding.rvEditingThumbnails.adapter = thumbnailAdapter
    }

    private fun loadPhotoIntoRenderer(uri: Uri) {
        renderer.setPhotoUri(uri)
        binding.glSurfaceView.requestRender()
    }
}