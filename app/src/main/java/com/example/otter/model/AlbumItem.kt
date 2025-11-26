package com.example.otter.model

/**
 * Represents an album/category of photos.
 * @param name The name of the album (e.g., "All", "Camera", "Screenshots").
 * @param isSelected Whether this album is currently selected.
 */
data class AlbumItem(
    val name: String,
    var isSelected: Boolean = false
)
