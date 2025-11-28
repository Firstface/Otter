package com.example.otter.model

import androidx.annotation.StringRes
import com.example.otter.R

enum class EditingToolType(@StringRes val titleRes: Int) {
    SCALE(R.string.tool_scale),
    PAN(R.string.tool_pan),
    CROP(R.string.tool_crop),
    BRUSH(R.string.tool_brush),
}
