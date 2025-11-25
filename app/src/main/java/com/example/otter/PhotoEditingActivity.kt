package com.example.otter

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.otter.databinding.ActivityPhotoeditingBinding

class PhotoEditingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoeditingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhotoeditingBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}