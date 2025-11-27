package com.example.otter

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.adapter.AlbumAdapter
import com.example.otter.adapter.FunctionAdapter
import com.example.otter.adapter.PhotoAdapter
import com.example.otter.databinding.ActivityPhotoSelectionBinding
import com.example.otter.model.AlbumItem
import com.example.otter.model.FunctionItem
import com.example.otter.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图片选择主界面，包含功能入口、相册列表和图片网格
 * * 修改记录：
 * - 新增列表点击自动居中功能 (smoothScrollToCenter)
 * - 优化边界滚动逻辑：无法居中时自动贴边，不留白
 */
class PhotoSelectionActivity : AppCompatActivity() {

    // ViewBinding 实例
    private lateinit var binding: ActivityPhotoSelectionBinding
    // 图片列表适配器
    private lateinit var photoAdapter: PhotoAdapter
    // 功能入口适配器
    private lateinit var functionAdapter: FunctionAdapter
    // 相册列表适配器
    private lateinit var albumAdapter: AlbumAdapter

    // 功能入口数据集合
    private val functions = mutableListOf<FunctionItem>()
    // 相册数据集合
    private val albums = mutableListOf<AlbumItem>()
    // 当前显示的所有照片数据
    private val allPhotos = mutableListOf<PhotoItem>()
    // 当前选中的相册名称（null 表示全部）
    private var currentAlbum: String? = null
    // 是否正在加载数据标志位
    private var isLoading = false
    // 分页加载每页数量
    private val pageSize = 100

    companion object {
        // 权限请求码
        private const val PERMISSION_REQUEST_CODE = 100
    }

    /**
     * 初始化界面基础配置
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        checkPermissionAndLoadPhotos()
    }

    /**
     * 设置顶部工具栏的返回按钮
     */
    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    /**
     * 初始化所有 RecyclerView 列表
     */
    private fun setupRecyclerViews() {
        // Functions RecyclerView (顶部功能区)
        setupFunctionRecyclerView()
        // Albums RecyclerView (相册选择区)
        setupAlbumRecyclerView()
        // Photos RecyclerView (图片展示区)
        setupPhotosRecyclerView()
    }

    /**
     * 平滑滚动到指定位置居中显示
     * @param recyclerView 要滚动的 RecyclerView
     * @param position 目标位置索引
     */
    private fun smoothScrollToCenter(recyclerView: RecyclerView, position: Int) {
        // 1. 基础检查
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (position < 0 || position >= itemCount) return
        val lm = recyclerView.layoutManager ?: return

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {

            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                // step 0: 【铺满检测】(新增逻辑)
                // 检查列表是否"太短"，完全在屏幕内
                val firstView = lm.findViewByPosition(0)
                val lastView = lm.findViewByPosition(itemCount - 1)

                // 只有当第0个和最后一个Item同时在屏幕上，才有可能"不够铺满"
                if (firstView != null && lastView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val lastRight = lm.getDecoratedRight(lastView)

                    // 如果 第0个在左边界内(或压线) 并且 最后一个在右边界内(或压线)
                    // 说明内容总宽度 <= 屏幕宽度 -> 禁止滚动！
                    if (firstLeft >= boxStart && lastRight <= boxEnd) {
                        return 0
                    }
                }

                // --- 以下是之前的逻辑 ---

                // step 1: 计算理论居中位移
                val boxCenter = boxStart + (boxEnd - boxStart) / 2
                val viewCenter = viewStart + (viewEnd - viewStart) / 2
                val dtCentered = boxCenter - viewCenter

                // step 2: 左边界预测
                if (firstView != null) {
                    val firstLeft = lm.getDecoratedLeft(firstView)
                    val finalFirstLeft = firstLeft + dtCentered
                    if (finalFirstLeft > boxStart) {
                        return boxStart - firstLeft
                    }
                }

                // step 3: 右边界预测
                // 注意：这里需要重新获取一下 lastView，因为上面 step 0 可能为 null (长列表时)
                val checkedLastView = lm.findViewByPosition(itemCount - 1)
                if (checkedLastView != null) {
                    val lastRight = lm.getDecoratedRight(checkedLastView)
                    val finalLastRight = lastRight + dtCentered
                    if (finalLastRight < boxEnd) {
                        return boxEnd - lastRight
                    }
                }

                // step 4: 安全，执行居中
                return dtCentered
            }

            // 保持丝滑配置
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

    /**
     * 初始化功能入口水平列表
     * 1. 从 intent 获取预选功能项
     * 2. 初始化默认功能列表
     * 3. 设置单选效果 + 自动居中
     */
    private fun setupFunctionRecyclerView() {
        val selectedFunctionName = intent.getStringExtra("SELECTED_FUNCTION_NAME")
        functions.clear()

        functions.addAll(listOf(
            FunctionItem("随机"),
            FunctionItem("修图"),
            FunctionItem("视频剪辑"),
            FunctionItem("导入"),
            FunctionItem("修实况Live"),
            FunctionItem("人像美化"),
            FunctionItem("拼图"),
            FunctionItem("批量修图"),
            FunctionItem("画质超清"),
            FunctionItem("魔法消除"),
            FunctionItem("智能抠图"),
            FunctionItem("一键出片"),
            FunctionItem("一键美化"),
        ))

        // 查找预选位置
        var selectedIndex = -1
        functions.forEachIndexed { index, item ->
            if (item.name == selectedFunctionName) {
                item.isSelected = true
                selectedIndex = index
            }
        }

        functionAdapter = FunctionAdapter(functions) { position ->
            // 点击事件处理
            functions.forEachIndexed { index, item ->
                item.isSelected = index == position
            }
            functionAdapter.notifyDataSetChanged()

            smoothScrollToCenter(binding.rvFunctions, position)
        }
        binding.rvFunctions.adapter = functionAdapter

        // 如果有预选功能，进入页面时自动滚动到该位置
        // 使用 post 是为了等待 Layout 计算完成，否则滚动可能无效
        if (selectedIndex != -1) {
            binding.rvFunctions.post {
                smoothScrollToCenter(binding.rvFunctions, selectedIndex)
            }
        }
    }

    /**
     * 初始化相册列表
     * 1. 处理相册选择事件
     * 2. 选择后重新加载对应照片 + 自动居中
     */
    private fun setupAlbumRecyclerView() {
        albumAdapter = AlbumAdapter(albums) { position ->
            // 1. 更新选中状态
            albums.forEachIndexed { index, item ->
                item.isSelected = index == position
            }
            albumAdapter.notifyDataSetChanged()

            // 2. 【新增】点击后自动滑动该项到中间
            smoothScrollToCenter(binding.rvAlbums, position)

            // 3. 重新加载数据
            currentAlbum = albums[position].name
            loadInitialPhotos()
        }
        binding.rvAlbums.adapter = albumAdapter
    }

    /**
     * 初始化图片网格列表
     * 1. 配置适配器
     * 2. 添加滚动监听实现分页加载
     */
    private fun setupPhotosRecyclerView() {
        photoAdapter = PhotoAdapter()
        binding.rvPhotos.adapter = photoAdapter
        binding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    loadMorePhotos()
                }
            }
        })
    }

    /**
     * 检查并请求存储权限
     * 有权限时直接加载数据
     */
    private fun checkPermissionAndLoadPhotos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
        } else {
            loadInitialPhotos()
            loadAlbums()
        }
    }

    /**
     * 权限请求结果回调处理
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadInitialPhotos()
            loadAlbums()
        }
    }

    /**
     * 初始化加载照片（从第0张开始）
     */
    private fun loadInitialPhotos() {
        loadPhotos(0)
    }

    /**
     * 加载更多照片（分页加载）
     */
    private fun loadMorePhotos() {
        loadPhotos(allPhotos.size)
    }

    /**
     * 核心加载照片方法
     * @param offset 分页偏移量
     * 1. 使用 MediaStore 查询系统图片
     * 2. 按拍摄时间升序排序
     * 3. 支持按相册过滤
     * 4. 分页加载机制
     */
    private fun loadPhotos(offset: Int) {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            val photos = mutableListOf<PhotoItem>()
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            val queryArgs = Bundle().apply {
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_TAKEN))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, 1)
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                if (currentAlbum != null && currentAlbum != "全部") {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(currentAlbum))
                }
            }

            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, queryArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photos.add(PhotoItem(contentUri))
                }
            }

            withContext(Dispatchers.Main) {
                if (offset == 0) {
                    allPhotos.clear()
                }
                allPhotos.addAll(photos)
                photoAdapter.submitList(allPhotos.toList())
                isLoading = false
            }
        }
    }

    /**
     * 加载设备相册列表
     * 1. 获取所有包含图片的相册
     * 2. 自动添加"全部"选项
     */
    private fun loadAlbums() {
        lifecycleScope.launch(Dispatchers.IO) {
            val albumSet = mutableSetOf<String>()
            val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketColumn)
                    if (bucketName != null) albumSet.add(bucketName)
                }
            }

            withContext(Dispatchers.Main) {
                albums.clear()
                albums.add(AlbumItem("全部", true))
                albums.addAll(albumSet.map { AlbumItem(it) })
                albumAdapter.notifyDataSetChanged()
            }
        }
    }
}