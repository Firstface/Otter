package com.example.otter.util

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.otter.model.AlbumItem
import com.example.otter.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A utility object for loading media from the Android MediaStore.
 * It includes best practices for permission handling, version compatibility,
 * and performance.
 */
object MediaLoader {

    private const val PAGE_SIZE = 50

    /**
     * Returns the appropriate storage permission required for the current Android version.
     */
    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Checks if the app has the necessary storage permission.
     */
    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, getRequiredPermission()) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Loads a list of photo albums (buckets) from the MediaStore.
     */
    suspend fun loadAlbums(context: Context): List<AlbumItem> = withContext(Dispatchers.IO) {
        val albumSet = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val contentResolver = context.contentResolver

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, // No specific selection, we want all albums
            null, // No selection args
            null  // Default sort order
        )?.use { cursor ->
            val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (bucketColumn != -1) {
                while (cursor.moveToNext()) {
                    cursor.getString(bucketColumn)?.let { albumSet.add(it) }
                }
            }
        }

        val albumList = mutableListOf(AlbumItem("全部", true))
        albumList.addAll(albumSet.map { AlbumItem(it) })
        albumList
    }

    /**
     * Loads a paginated list of photos from the MediaStore.
     *
     * @param context The application context.
     * @param page The page number to load (0-indexed).
     * @param albumName Optional: The name of the album to filter by.
     * @return A list of [PhotoItem]s.
     */
    suspend fun loadPhotos(
        context: Context,
        page: Int,
        albumName: String? = null
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<PhotoItem>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val contentResolver = context.contentResolver

        // Use a bundle for modern, efficient querying (recommended for API 26+)
        val queryArgs = Bundle().apply {
            // Sort by date taken in descending order to show newest first
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_TAKEN))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)

            // Pagination
            putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
            putInt(ContentResolver.QUERY_ARG_OFFSET, page * PAGE_SIZE)

            // Selection
            val selectionClauses = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()

            // Filter by MIME_TYPE to ensure we only get images.
            selectionClauses.add("${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'")

            // Filter by album if specified
            if (albumName != null && albumName != "全部") {
                selectionClauses.add("${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?")
                selectionArgs.add(albumName)
            }

            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionClauses.joinToString(" AND "))
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
        }

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
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photoList.add(PhotoItem(uri = contentUri))
                }
            }
        }
        photoList
    }
}