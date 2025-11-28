package com.example.otter

import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
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

    companion object {
        const val EXTRA_PHOTO_URIS = "EXTRA_PHOTO_URIS"
        const val EXTRA_FUNCTION_TYPE = "EXTRA_FUNCTION_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

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

    private fun setupViews() {
        renderer = PhotoRenderer(this)
        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        val tools = EditingToolType.values().toList()
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