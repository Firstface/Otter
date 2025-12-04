package com.example.otter.model

import androidx.annotation.StringRes
import com.example.otter.R

/**
 * 编辑工具类型枚举类，定义了应用中可用的编辑工具类型
 * @param titleRes 工具类型的字符串资源ID，用于显示在用户界面上
 */
enum class EditingToolType(@StringRes val titleRes: Int) {
    CROP(R.string.tool_crop),
    BRUSH(R.string.tool_brush),
}
