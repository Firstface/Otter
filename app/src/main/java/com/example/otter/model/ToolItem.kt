package com.example.otter.model

import androidx.annotation.DrawableRes

/**
 * Represents a tool item in the editor.
 * @param name The name of the tool to be displayed.
 * @param iconResId The drawable resource ID for the tool's icon.
 */
data class ToolItem(
    val name: String,
    @DrawableRes val iconResId: Int
)
