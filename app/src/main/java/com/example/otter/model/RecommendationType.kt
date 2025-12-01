package com.example.otter.model

import androidx.annotation.DrawableRes
import com.example.otter.R

/**
 * Represents the different recommendation items on the home screen.
 * @param imageResId The drawable resource for the recommendation item.
 * @param description The display text for the recommendation item.
 */
enum class RecommendationType(@DrawableRes val imageResId: Int, val description: String) {
    SEASIDE(R.mipmap.samplerecomendationpic, "海边风景"),
    CITY_NIGHT(R.mipmap.samplerecomendationpic, "城市夜景"),
    FOREST(R.mipmap.samplerecomendationpic, "静谧森林"),
    SUNSET(R.mipmap.samplerecomendationpic, "日落黄昏"),
    SNOWY_MOUNTAIN(R.mipmap.samplerecomendationpic, "雪山之巅")
}