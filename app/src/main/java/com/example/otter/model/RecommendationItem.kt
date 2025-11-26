package com.example.otter.model

import androidx.annotation.DrawableRes

/**
 * Represents a recommendation item in the list.
 * @param imageResId The drawable resource ID for the recommendation image.
 * @param description The description text below the image.
 */
data class RecommendationItem(
    @DrawableRes val imageResId: Int,
    val description: String
)
