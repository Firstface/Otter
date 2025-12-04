package com.example.otter.model

/**
 * 工具项数据类，用于表示编辑工具中的项
 * @param type 工具项的类型（例如，"裁剪"、"滤镜"等）
 */
data class ToolItem(
    val type: ToolType
)
