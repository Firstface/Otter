package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemRecommendationBinding
import com.example.otter.model.RecommendationItem

class RecommendationAdapter(private val recommendationList: List<RecommendationItem>) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recommendationList[position]
        holder.binding.ivRecommendationImage.setImageResource(item.imageResId)
        holder.binding.tvRecommendationDescription.text = item.description
    }

    override fun getItemCount(): Int {
        return recommendationList.size
    }
}