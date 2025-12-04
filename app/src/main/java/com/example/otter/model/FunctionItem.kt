package com.example.otter.model


/**
 * 功能项数据类，用于表示顶部栏中的功能项
 * @param type 功能项的类型（例如，"相册"、"编辑工具"等）
 * @param isSelected 是否当前选中该功能项（默认为false）
 */
data class FunctionItem(
    val type: FunctionType,
    var isSelected: Boolean = false
)
