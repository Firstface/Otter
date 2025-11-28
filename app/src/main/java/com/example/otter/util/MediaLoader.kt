package com.example.otter.util

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.otter.model.AlbumItem
import com.example.otter.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 媒体加载工具类
 * 适配了 Android 10 (分区存储) 和 Android 14 (部分权限访问)
 */
object MediaLoader {

    private const val TAG = "MediaLoader"
    private const val PAGE_SIZE = 50

    /**
     * 获取当前系统版本需要申请的权限数组
     * 建议在 Activity/Fragment 中使用此方法来决定 requestPermissions 传什么
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            // Android 14 (API 34)+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            // Android 13 (API 33)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            // Android 10-12
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * 检查是否有读取相册的权限
     * 关键修改：兼容 Android 14 的“部分权限”状态
     */
    fun hasPermission(context: Context): Boolean {
        // 1. Android 13+ (API 33) 的基础检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasFullAccess = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            // 如果已经有全部权限，直接返回 true
            if (hasFullAccess) return true

            // 2. Android 14+ (API 34) 特有的“部分权限”检查
            // 当用户选择“Select Photos”时，READ_MEDIA_IMAGES 是 false，但这个是 true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasPartialAccess = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPartialAccess) {
                    Log.d(TAG, "检测到 Android 14 部分访问权限 (Partial Access)")
                    return true
                }
            }

            return false
        }

        // 3. Android 12及以下检查逻辑
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 加载相册列表（文件夹）
     */
    suspend fun loadAlbums(context: Context): List<AlbumItem> = withContext(Dispatchers.IO) {
        val albumSet = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        // 这里的 selection 即使在“部分权限”下也不用改，系统会自动过滤
        val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'"
        val contentResolver = context.contentResolver

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                if (bucketColumn != -1) {
                    while (cursor.moveToNext()) {
                        cursor.getString(bucketColumn)?.let { albumSet.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading albums", e)
        }

        val albumList = mutableListOf(AlbumItem("全部", true))
        albumList.addAll(albumSet.map { AlbumItem(it) })
        albumList
    }

    /**
     * 分页加载图片
     */
    suspend fun loadPhotos(
        context: Context,
        page: Int,
        albumName: String? = null
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<PhotoItem>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val contentResolver = context.contentResolver

        // 构建查询参数 Bundle (API 26+ 推荐方式)
        val queryArgs = Bundle().apply {
            // 按时间倒序
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_TAKEN))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)

            // 分页
            putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
            putInt(ContentResolver.QUERY_ARG_OFFSET, page * PAGE_SIZE)

            // 筛选条件
            val selectionClauses = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()

            selectionClauses.add("${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'")

            if (albumName != null && albumName != "全部") {
                selectionClauses.add("${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?")
                selectionArgs.add(albumName)
            }

            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionClauses.joinToString(" AND "))
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
        }

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                queryArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                if (idColumn != -1) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        // 使用 ID 拼接 Uri，适配 Android 10+
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        photoList.add(PhotoItem(uri = contentUri))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading photos", e)
        }

        photoList
    }
}