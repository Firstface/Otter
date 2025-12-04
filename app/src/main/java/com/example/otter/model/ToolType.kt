package com.example.otter.model

import androidx.annotation.DrawableRes
import com.example.otter.R

/**
 * 工具类型枚举类，定义了应用中可用的编辑工具类型
 * @param displayName 工具类型的显示名称，用于在用户界面上展示
 * @param iconResId 工具类型的图标资源ID，用于在用户界面上展示
 */
enum class ToolType(val displayName: String, @DrawableRes val iconResId: Int) {
    RANDOM("随机", R.drawable.ic_linked_camera),
    BATCH_EDIT("批量修图", R.drawable.ic_batch_edit),
    SUPER_RESOLUTION("画质超清", R.drawable.ic_hd),
    MAGIC_ERASER("魔法消除", R.drawable.ic_eraser),
    SMART_CUTOUT("智能抠图", R.drawable.ic_cutout),
    ONE_CLICK_MOVIE("一键出片", R.drawable.ic_film),
    ONE_CLICK_BEAUTY("一键美化", R.drawable.ic_magic),
    ALL_TOOLS("所有工具", R.drawable.ic_grid_all)
}