package com.example.otter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.adapter.AlbumAdapter
import com.example.otter.adapter.FunctionAdapter
import com.example.otter.adapter.PhotoAdapter
import com.example.otter.databinding.ActivityPhotoSelectionBinding
import com.example.otter.viewmodel.PhotoSelectionEvent
import com.example.otter.viewmodel.PhotoSelectionViewModel
import kotlinx.coroutines.launch

class PhotoSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoSelectionBinding
    private val viewModel: PhotoSelectionViewModel by viewModels()

    private lateinit var photoAdapter: PhotoAdapter

    // ** FIX: Flag to prevent multiple initial scrolls **
    private var hasScrolledToInitialFunction = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        const val SELECTED_FUNCTION_NAME = "SELECTED_FUNCTION_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        observeViewModel()

        checkPermissionAndLoadData()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        photoAdapter = PhotoAdapter()
        binding.rvPhotos.adapter = photoAdapter
        binding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.onScrolledToEnd()
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.functions.collect { functions ->
                        if (functions.isNotEmpty()) {
                            val scrollState = binding.rvFunctions.layoutManager?.onSaveInstanceState()
                            val functionAdapter = FunctionAdapter(functions) { position ->
                                viewModel.onFunctionClick(position)
                            }
                            binding.rvFunctions.adapter = functionAdapter
                            binding.rvFunctions.layoutManager?.onRestoreInstanceState(scrollState)

                            // ** FIX: Handle initial scroll here **
                            if (!hasScrolledToInitialFunction) {
                                val selectedIndex = functions.indexOfFirst { it.isSelected }
                                if (selectedIndex != -1) {
                                    binding.rvFunctions.post { smoothScrollToCenter(binding.rvFunctions, selectedIndex) }
                                    hasScrolledToInitialFunction = true
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.albums.collect { albums ->
                        if (albums.isNotEmpty()) {
                            val scrollState = binding.rvAlbums.layoutManager?.onSaveInstanceState()
                            val albumAdapter = AlbumAdapter(albums) { position ->
                                viewModel.onAlbumClick(position)
                            }
                            binding.rvAlbums.adapter = albumAdapter
                            binding.rvAlbums.layoutManager?.onRestoreInstanceState(scrollState)
                        }
                    }
                }

                launch {
                    viewModel.photos.collect { photos ->
                        photoAdapter.submitList(photos)
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is PhotoSelectionEvent.ScrollFunctionList -> {
                                // This now only handles subsequent clicks
                                binding.rvFunctions.post { smoothScrollToCenter(binding.rvFunctions, event.position) }
                            }
                            is PhotoSelectionEvent.ScrollAlbumList -> {
                                binding.rvAlbums.post { smoothScrollToCenter(binding.rvAlbums, event.position) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionAndLoadData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
        } else {
            viewModel.initialize(intent.getStringExtra(SELECTED_FUNCTION_NAME))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.initialize(intent.getStringExtra(SELECTED_FUNCTION_NAME))
        }
    }

    private fun smoothScrollToCenter(recyclerView: RecyclerView, position: Int) {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (position < 0 || position >= itemCount) return
        val lm = recyclerView.layoutManager ?: return

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                val firstView = lm.findViewByPosition(0)
                val lastView = lm.findViewByPosition(itemCount - 1)

                if (firstView != null && lastView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val lastRight = lm.getDecoratedRight(lastView)
                    if (firstLeft >= boxStart && lastRight <= boxEnd) {
                        return 0
                    }
                }

                val boxCenter = boxStart + (boxEnd - boxStart) / 2
                val viewCenter = viewStart + (viewEnd - viewStart) / 2
                val dtCentered = boxCenter - viewCenter

                if (firstView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val finalFirstLeft = firstLeft + dtCentered
                    if (finalFirstLeft > boxStart) {
                        return boxStart - firstLeft
                    }
                }

                val checkedLastView = lm.findViewByPosition(itemCount - 1)
                if (checkedLastView != null) {
                    val lastRight = lm.getDecoratedRight(checkedLastView)
                    val finalLastRight = lastRight + dtCentered
                    if (finalLastRight < boxEnd) {
                        return boxEnd - lastRight
                    }
                }

                return dtCentered
            }

            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                return 100f / displayMetrics.densityDpi
            }

            override fun calculateTimeForDeceleration(dx: Int): Int {
                return (super.calculateTimeForDeceleration(dx) * 1.5).toInt()
            }
        }

        smoothScroller.targetPosition = position
        lm.startSmoothScroll(smoothScroller)
    }
}