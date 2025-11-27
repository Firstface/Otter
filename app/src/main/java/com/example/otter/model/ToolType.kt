package com.example.otter.model

import androidx.annotation.DrawableRes
import com.example.otter.R

/**
 * Represents the different tools available on the home screen.
 * @param displayName The name of the tool to be displayed.
 * @param iconResId The drawable resource ID for the tool's icon.
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