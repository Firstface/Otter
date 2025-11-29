package com.example.otter.model

import androidx.annotation.StringRes
import com.example.otter.R

enum class EditingToolType(@StringRes val titleRes: Int) {
    CROP(R.string.tool_crop),
    BRUSH(R.string.tool_brush),
}
