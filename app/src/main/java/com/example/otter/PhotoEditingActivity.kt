package com.example.otter

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.otter.adapter.EditingThumbnailAdapter
import com.example.otter.databinding.ActivityPhotoEditingBinding
import com.example.otter.model.FunctionType
import androidx.core.net.toUri

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditingBinding


    companion object {
        const val EXTRA_PHOTO_URIS = "EXTRA_PHOTO_URIS"
        const val EXTRA_FUNCTION_TYPE = "EXTRA_FUNCTION_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhotoEditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photoUriStrings = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS)
        val functionType = intent.getStringExtra(EXTRA_FUNCTION_TYPE)

        if (photoUriStrings.isNullOrEmpty()) {
            // Handle error: no URIs passed.
            finish()
            return
        }

        val photoUris = photoUriStrings.map { it.toUri() }

        // Load the first photo by default
        loadPhotoIntoMainView(photoUris[0])

        if (functionType == FunctionType.BATCH_EDIT.name) {
            setupBatchEditMode(photoUris)
        }
    }

    private fun setupBatchEditMode(photoUris: List<Uri>) {
        binding.rvEditingThumbnails.visibility = View.VISIBLE
        val thumbnailAdapter = EditingThumbnailAdapter(photoUris) { uri ->
            loadPhotoIntoMainView(uri)
        }
        binding.rvEditingThumbnails.adapter = thumbnailAdapter
    }

    private fun loadPhotoIntoMainView(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(binding.ivMainImage)
    }
}