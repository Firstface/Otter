package com.example.otter.model

import androidx.annotation.DrawableRes
import com.example.otter.R

/**
 * 推荐类型枚举类，定义了应用中可用的推荐类型
 * @param imageResId 推荐类型的图标资源ID，用于在用户界面上展示
 * @param description 推荐类型的描述文本，用于在用户界面上展示
 */
enum class RecommendationType(@DrawableRes val imageResId: Int, val description: String) {
    SEASIDE(R.mipmap.samplerecomendationpic, "海边风景"),
    CITY_NIGHT(R.mipmap.samplerecomendationpic, "城市夜景"),
    FOREST(R.mipmap.samplerecomendationpic, "静谧森林"),
    SUNSET(R.mipmap.samplerecomendationpic, "日落黄昏"),
    SNOWY_MOUNTAIN(R.mipmap.samplerecomendationpic, "雪山之巅")
}