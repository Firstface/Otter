package com.example.otter

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import android.content.ContentResolver

class PhotoSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoSelectionBinding
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var functionAdapter: FunctionAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private val functions = mutableListOf<FunctionItem>()
    private val albums = mutableListOf<AlbumItem>()
    private val allPhotos = mutableListOf<PhotoItem>()
    private var currentAlbum: String? = null
    private var isLoading = false
    private val pageSize = 100

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        checkPermissionAndLoadPhotos()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerViews() {
        // Functions RecyclerView
        setupFunctionRecyclerView()
        // Albums RecyclerView
        setupAlbumRecyclerView()
        // Photos RecyclerView
        setupPhotosRecyclerView()
    }
    
    private fun setupFunctionRecyclerView() {
        val selectedFunctionName = intent.getStringExtra("SELECTED_FUNCTION_NAME")
        functions.addAll(listOf(
            FunctionItem("修图"), FunctionItem("拼图"), FunctionItem("视频剪辑"),
            FunctionItem("导入"), FunctionItem("相机"), FunctionItem("修实况Live"), FunctionItem("人像美化")
        ))
        functions.find { it.name == selectedFunctionName }?.isSelected = true

        functionAdapter = FunctionAdapter(functions) { position ->
            functions.forEachIndexed { index, item ->
                item.isSelected = index == position
            }
            functionAdapter.notifyDataSetChanged()
        }
        binding.rvFunctions.adapter = functionAdapter
    }

    private fun setupAlbumRecyclerView() {
        albumAdapter = AlbumAdapter(albums) { position ->
            albums.forEachIndexed { index, item ->
                item.isSelected = index == position
            }
            albumAdapter.notifyDataSetChanged()

            currentAlbum = albums[position].name
            loadInitialPhotos()
        }
        binding.rvAlbums.adapter = albumAdapter
    }
    
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

    private fun checkPermissionAndLoadPhotos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
        } else {
            loadInitialPhotos()
            loadAlbums()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadInitialPhotos()
            loadAlbums()
        }
    }

    private fun loadInitialPhotos() {
        loadPhotos(0)
    }

    private fun loadMorePhotos() {
        loadPhotos(allPhotos.size)
    }

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