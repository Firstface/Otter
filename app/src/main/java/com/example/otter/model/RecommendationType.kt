package com.example.otter.model

import androidx.annotation.DrawableRes
import com.example.otter.R

/**
 * Represents the different recommendation items on the home screen.
 * @param imageResId The drawable resource for the recommendation item.
 * @param description The display text for the recommendation item.
 */
enum class RecommendationType(@DrawableRes val imageResId: Int, val description: String) {
    SEASIDE(R.mipmap.fbb0ed97094a94b331c72121bf5a907, "海边风景"),
    CITY_NIGHT(R.mipmap.fbb0ed97094a94b331c72121bf5a907, "城市夜景"),
    FOREST(R.mipmap.fbb0ed97094a94b331c72121bf5a907, "静谧森林"),
    SUNSET(R.mipmap.fbb0ed97094a94b331c72121bf5a907, "日落黄昏"),
    SNOWY_MOUNTAIN(R.mipmap.fbb0ed97094a94b331c72121bf5a907, "雪山之巅")
}