package com.example.otter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import com.example.otter.adapter.SelectedPhotoAdapter
import com.example.otter.databinding.ActivityPhotoSelectionBinding
import com.example.otter.util.MediaLoader
import com.example.otter.viewmodel.PhotoSelectionEvent
import com.example.otter.viewmodel.PhotoSelectionViewModel
import kotlinx.coroutines.launch

class PhotoSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoSelectionBinding
    private val viewModel: PhotoSelectionViewModel by viewModels()

    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var selectedPhotoAdapter: SelectedPhotoAdapter
    private lateinit var albumAdapter: AlbumAdapter

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
        setupClickListeners()
        observeViewModel()

        viewModel.initFunctions(intent.getStringExtra(SELECTED_FUNCTION_NAME))
        checkPermissionAndLoadData()
    }

    override fun onResume() {
        super.onResume()
        if (MediaLoader.hasPermission(this)) {
            viewModel.loadMedia()
        }
        checkManageButtonVisibility()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        photoAdapter = PhotoAdapter { photo ->
            viewModel.onPhotoClick(photo)
        }
        binding.rvPhotos.adapter = photoAdapter
        binding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.onScrolledToEnd()
                }
            }
        })

        selectedPhotoAdapter = SelectedPhotoAdapter { photo ->
            viewModel.onPhotoClick(photo)
        }
        binding.rvSelectedPhotos.adapter = selectedPhotoAdapter

        albumAdapter = AlbumAdapter { position ->
            viewModel.onAlbumClick(position)
        }
        binding.rvAlbums.adapter = albumAdapter
    }

    private fun setupClickListeners() {
        binding.btnDone.setOnClickListener {
            viewModel.onDoneClick()
        }
        binding.tvManageSelection.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            }
        }
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
                        albumAdapter.submitList(albums)
                    }
                }

                launch {
                    viewModel.photos.collect { photos ->
                        photoAdapter.submitList(photos)
                    }
                }

                launch {
                    viewModel.selectedPhotos.collect { photos ->
                        selectedPhotoAdapter.submitList(photos)
                        binding.tvSelectedCount.text = "已选择 ${photos.size}/9"
                    }
                }

                launch {
                    viewModel.isBatchEditMode.collect { isBatchEditMode ->
                        binding.selectedPhotosContainer.visibility = if (isBatchEditMode) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is PhotoSelectionEvent.ScrollFunctionList -> {
                                binding.rvFunctions.post { smoothScrollToCenter(binding.rvFunctions, event.position) }
                            }
                            is PhotoSelectionEvent.ScrollAlbumList -> {
                                binding.rvAlbums.post { smoothScrollToCenter(binding.rvAlbums, event.position) }
                            }
                            is PhotoSelectionEvent.NavigateToPhotoEditing -> {
                                val intent = Intent(this@PhotoSelectionActivity, PhotoEditingActivity::class.java).apply {
                                    putStringArrayListExtra(PhotoEditingActivity.EXTRA_PHOTO_URIS, event.photoUris)
                                    putExtra(PhotoEditingActivity.EXTRA_FUNCTION_TYPE, event.functionType)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionAndLoadData() {
        if (MediaLoader.hasPermission(this)) {
            viewModel.loadMedia()
        } else {
            requestPermissions()
        }
        checkManageButtonVisibility()
    }

    private fun requestPermissions() {
        // 直接调用 MediaLoader 的新方法，逻辑统一
        val permissionsToRequest = MediaLoader.getRequiredPermissions()
        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 直接使用工具类判断。
            // 因为 MediaLoader.hasPermission 已经完美处理了“全权”和“部分权限”两种情况
            if (MediaLoader.hasPermission(this)) {
                viewModel.loadMedia()
            } else {
                // 只有真的什么都没选（拒绝）时，才清空
                viewModel.clearMedia()
                // 可选：在这里弹窗提示用户去设置里开启权限
            }
            checkManageButtonVisibility()
        }
    }

    private fun checkManageButtonVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            val hasFullAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            binding.tvManageSelection.visibility = if (hasFullAccess) View.GONE else View.VISIBLE
        } else {
            binding.tvManageSelection.visibility = View.GONE
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