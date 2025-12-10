package com.example.otter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.otter.adapter.SelectedPhotoAdapter
import com.example.otter.databinding.ActivityPhotoSelectionBinding
import com.example.otter.util.MediaLoader
import com.example.otter.viewmodel.PhotoSelectionEvent
import com.example.otter.viewmodel.PhotoSelectionViewModel
import kotlinx.coroutines.launch

/**
 * 照片选择 Activity
 *
 * 功能描述：
 * 1. 展示设备上的相册和照片网格。
 * 2. 支持单选（直接跳转编辑）和多选（批量编辑模式）。
 * 3. 底部功能栏切换（如：修图、拼图、批量处理等）。
 * 4. 适配 Android 14 (UPSIDE_DOWN_CAKE) 的部分媒体权限 (READ_MEDIA_VISUAL_USER_SELECTED)。
 */
class PhotoSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoSelectionBinding
    // 使用 viewModels 委托获取 ViewModel 实例
    private val viewModel: PhotoSelectionViewModel by viewModels()

    // --- Adapters ---
    private lateinit var photoAdapter: PhotoAdapter // 照片网格适配器
    private lateinit var selectedPhotoAdapter: SelectedPhotoAdapter // 已选照片预览适配器 (多选模式下显示)
    private lateinit var albumAdapter: AlbumAdapter // 相册列表适配器

    // 标记是否已经自动滚动到初始选中的功能 Tab，防止重复滚动
    private var hasScrolledToInitialFunction = false

    // 注册 Activity 结果回调，用于处理从编辑页返回的情况
    private val photoEditingResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 如果编辑页保存了新图片，返回时需要刷新列表以显示新图片
            if (MediaLoader.hasPermission(this)) {
                viewModel.loadMedia()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        const val SELECTED_FUNCTION_NAME = "SELECTED_FUNCTION_NAME" // Intent key: 初始选中的功能
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        // 初始化 ViewModel，传入从上一个页面带来的功能选项
        viewModel.initFunctions(intent.getStringExtra(SELECTED_FUNCTION_NAME))
        // 检查权限并加载数据
        checkPermissionAndLoadData()
    }

    /**
     * onResume 生命周期
     * 处理场景：用户切到系统设置修改了权限（例如从“不允许”改为“允许”），切回应用时需要立刻刷新
     */
    override fun onResume() {
        super.onResume()
        if (MediaLoader.hasPermission(this)) {
            viewModel.loadMedia()
        }
        // 检查是否需要显示“管理已选照片”按钮 (Android 14 特性)
        checkManageButtonVisibility()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // 1. 照片网格列表
        photoAdapter = PhotoAdapter { photo ->
            viewModel.onPhotoClick(photo) // 点击照片
        }
        binding.rvPhotos.adapter = photoAdapter
        // 添加滚动监听，实现分页加载 (Infinite Scrolling)
        binding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 检查是否滚动到底部 (1 表示向下滑动方向)
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.onScrolledToEnd() // 通知 ViewModel 加载下一页
                }
            }
        })

        // 2. 已选照片列表 (底部/顶部显示的横向预览条)
        selectedPhotoAdapter = SelectedPhotoAdapter { photo ->
            viewModel.onPhotoClick(photo) // 点击已选照片通常是取消选择
        }
        binding.rvSelectedPhotos.adapter = selectedPhotoAdapter

        // 3. 相册列表
        albumAdapter = AlbumAdapter { position ->
            viewModel.onAlbumClick(position) // 切换相册
        }
        binding.rvAlbums.adapter = albumAdapter
    }

    private fun setupClickListeners() {
        // 完成按钮 (多选模式下确认选择)
        binding.btnDone.setOnClickListener {
            viewModel.onDoneClick()
        }

        // "管理选择" 按钮：仅在 Android 14+ 且用户只授予了“部分访问权限”时使用
        // 点击后系统会再次弹出权限框，允许用户把更多照片加入到应用的访问范围内
        binding.tvManageSelection.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            }
        }
    }

    /**
     * 观察 ViewModel 的状态流 (Flow)
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            // 使用 repeatOnLifecycle 确保只在 Activity 处于 STARTED 状态时收集数据，节省资源
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. 观察功能列表 (底部 Tab)
                launch {
                    viewModel.functions.collect { functions ->
                        if (functions.isNotEmpty()) {
                            // 保存滚动状态，防止刷新列表时跳动
                            val scrollState = binding.rvFunctions.layoutManager?.onSaveInstanceState()
                            val functionAdapter = FunctionAdapter(functions) { position ->
                                viewModel.onFunctionClick(position)
                            }
                            binding.rvFunctions.adapter = functionAdapter
                            binding.rvFunctions.layoutManager?.onRestoreInstanceState(scrollState)

                            // 首次加载时，自动滚动到选中的功能位置
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

                // 2. 观察相册列表
                launch {
                    viewModel.albums.collect { albums ->
                        albumAdapter.submitList(albums)
                    }
                }

                // 3. 观察照片列表
                launch {
                    viewModel.photos.collect { photos ->
                        photoAdapter.submitList(photos)
                    }
                }

                // 4. 观察已选中的照片 (用于更新底部预览条和计数)
                launch {
                    viewModel.selectedPhotos.collect { photos ->
                        selectedPhotoAdapter.submitList(photos)
                        binding.tvSelectedCount.text =  "已选择 ${photos.size}/9"
                    }
                }

                // 5. 观察是否处于批量编辑模式
                launch {
                    viewModel.isBatchEditMode.collect { isBatchEditMode ->
                        // 批量模式显示底部预览栏，否则隐藏
                        binding.selectedPhotosContainer.visibility = if (isBatchEditMode) View.VISIBLE else View.GONE
                    }
                }

                // 6. 观察一次性事件 (UI 动作)
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is PhotoSelectionEvent.ScrollFunctionList -> {
                                // 功能切换时，将选中项居中
                                binding.rvFunctions.post { smoothScrollToCenter(binding.rvFunctions, event.position) }
                            }
                            is PhotoSelectionEvent.ScrollAlbumList -> {
                                // 相册切换时，将选中项居中
                                binding.rvAlbums.post { smoothScrollToCenter(binding.rvAlbums, event.position) }
                            }
                            is PhotoSelectionEvent.NavigateToPhotoEditing -> {
                                // 跳转到编辑页面
                                val intent = Intent(this@PhotoSelectionActivity, PhotoEditingActivity::class.java).apply {
                                    putStringArrayListExtra(PhotoEditingActivity.EXTRA_PHOTO_URIS, event.photoUris)
                                    putExtra(PhotoEditingActivity.EXTRA_FUNCTION_TYPE, event.functionType)
                                }
                                photoEditingResultLauncher.launch(intent)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 权限管理逻辑 ---

    private fun checkPermissionAndLoadData() {
        if (MediaLoader.hasPermission(this)) {
            viewModel.loadMedia()
        } else {
            requestPermissions()
        }
        checkManageButtonVisibility()
    }

    private fun requestPermissions() {
        // 直接调用 MediaLoader 的工具方法，获取当前 Android 版本所需的权限数组
        val permissionsToRequest = MediaLoader.getRequiredPermissions()
        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // MediaLoader.hasPermission 封装了对“完全授权”和“部分授权”的判断
            // 只要用户给了一丁点权限（哪怕只是选了几张图），这里都会返回 true
            if (MediaLoader.hasPermission(this)) {
                viewModel.loadMedia()
            } else {
                // 只有用户彻底拒绝时，清空数据
                viewModel.clearMedia()
            }
            checkManageButtonVisibility()
        }
    }

    /**
     * 控制 "管理选择" 按钮的可见性
     * 仅在 Android 14+ 且用户选择了 "Select Photos..." (部分授权) 时显示
     */
    private fun checkManageButtonVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            // 检查是否拥有完全访问权限 (READ_MEDIA_IMAGES)
            // 如果没有完全权限，但 hasPermission 返回了 true，说明是“部分权限”状态
            val hasFullAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            // 如果有完全权限，隐藏按钮；如果是部分权限，显示按钮
            binding.tvManageSelection.visibility = if (hasFullAccess) View.GONE else View.VISIBLE
        } else {
            // 旧版本 Android 没有部分权限概念，始终隐藏
            binding.tvManageSelection.visibility = View.GONE
        }
    }

    /**
     * 自定义平滑滚动方法
     * 作用：将指定 position 的 Item 滚动到 RecyclerView 的正中间
     * 逻辑：计算 Item 中心点与 RecyclerView 中心点的距离，进行偏移滚动
     */
    private fun smoothScrollToCenter(recyclerView: RecyclerView, position: Int) {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (position < 0 || position >= itemCount) return
        val lm = recyclerView.layoutManager ?: return

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            // 计算滚动偏移量，确保目标 View 位于 Box (RecyclerView) 中间
            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                val firstView = lm.findViewByPosition(0)
                val lastView = lm.findViewByPosition(itemCount - 1)

                // 边界处理：如果内容不足以填满屏幕或已在边界，不强行居中，避免留白
                if (firstView != null && lastView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val lastRight = lm.getDecoratedRight(lastView)
                    if (firstLeft >= boxStart && lastRight <= boxEnd) {
                        return 0
                    }
                }

                val boxCenter = boxStart + (boxEnd - boxStart) / 2
                val viewCenter = viewStart + (viewEnd - viewStart) / 2
                val dtCentered = boxCenter - viewCenter // 核心计算：中心点差值

                // 处理左边界：防止向右滚动过度导致左侧留白
                if (firstView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val finalFirstLeft = firstLeft + dtCentered
                    if (finalFirstLeft > boxStart) {
                        return boxStart - firstLeft
                    }
                }

                // 处理右边界：防止向左滚动过度导致右侧留白
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

            // 控制滚动速度 (数值越大越慢)
            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                return 100f / displayMetrics.densityDpi
            }

            // 控制减速时间，使其看起来更平滑
            override fun calculateTimeForDeceleration(dx: Int): Int {
                return (super.calculateTimeForDeceleration(dx) * 1.5).toInt()
            }
        }

        smoothScroller.targetPosition = position
        lm.startSmoothScroll(smoothScroller)
    }
}