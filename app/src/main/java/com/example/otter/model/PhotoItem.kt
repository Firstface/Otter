package com.example.otter.model

import android.net.Uri


/**
 * 照片项数据类，用于表示相册/分类中的照片项
 * @param uri 照片的内容URI
 * @param isSelected 是否当前选中该照片（默认为false）
 */
data class PhotoItem(
    val uri: Uri,
    val isSelected: Boolean = false
)
