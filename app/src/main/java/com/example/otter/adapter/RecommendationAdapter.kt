package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemRecommendationBinding
import com.example.otter.model.RecommendationItem

/**
 * RecyclerView适配器，用于显示推荐信息列表
 * @param recommendationList 推荐项数据列表，包含每个推荐项目的图片资源和描述
 */
class RecommendationAdapter(private val recommendationList: List<RecommendationItem>) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    /**
     * ViewHolder用于缓存视图绑定对象
     * @param binding 列表项的视图绑定对象，包含图片和文本视图
     */
    inner class ViewHolder(val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * 创建ViewHolder，初始化列表项布局
     * @param parent 父视图组，通常是RecyclerView
     * @param viewType 视图类型，默认0
     * @return 包含视图绑定对象的ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用数据绑定工具初始化列表项布局
        val binding = ItemRecommendationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

     /**
     * 绑定数据到ViewHolder的视图
     * @param holder 要绑定数据的ViewHolder
     * @param position 数据项在列表中的位置
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 绑定数据到视图
        val item = recommendationList[position]
        holder.binding.ivRecommendationImage.setImageResource(item.type.imageResId)
        holder.binding.tvRecommendationDescription.text = item.type.description
    }
    /**
     * 返回推荐项数据列表的大小
     */
    override fun getItemCount(): Int {
        return recommendationList.size
    }
}