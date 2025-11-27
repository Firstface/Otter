package com.example.otter

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.otter.databinding.ActivityPhotoeditingBinding

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoeditingBinding

    companion object {
        const val EXTRA_PHOTO_URI = "EXTRA_PHOTO_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhotoeditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photoUriString = intent.getStringExtra(EXTRA_PHOTO_URI)
        if (photoUriString != null) {
            val photoUri = Uri.parse(photoUriString)
            Glide.with(this)
                .load(photoUri)
                .into(binding.ivMainImage)
        }
    }
}