package com.example.otter.model

/**
 * 推荐项数据类，用于表示推荐列表中的项
 * @param type 推荐项的类型（例如，"随机"、"修图"等）
 */
data class RecommendationItem(
    val type: RecommendationType
)
