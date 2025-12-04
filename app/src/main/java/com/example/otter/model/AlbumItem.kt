package com.example.otter.model

/**
 * 相册项数据类，用于表示相册/分类中的照片项
 * @param name 相册名称（例如，"所有"、"相机"、"截图"等）
 * @param isSelected 是否当前选中该相册（默认为false）
 */
data class AlbumItem(
    val name: String,
    var isSelected: Boolean = false
)
