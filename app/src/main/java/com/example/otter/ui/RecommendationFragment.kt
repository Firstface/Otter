package com.example.otter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.otter.R

/**
 * 推荐Fragment，展示用户推荐列表
 */
class RecommendationFragment : Fragment() {

    /**
     * 当视图被创建时，使用 Data Binding 绑定布局
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Just inflate the simple layout
        return inflater.inflate(R.layout.fragment_recommendation, container, false)
    }
}