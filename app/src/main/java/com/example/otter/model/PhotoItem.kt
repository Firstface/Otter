package com.example.otter.model

import android.net.Uri

/**
 * Represents a single photo item in the gallery.
 * @param uri The content URI of the photo.
 */
data class PhotoItem(
    val uri: Uri
)
