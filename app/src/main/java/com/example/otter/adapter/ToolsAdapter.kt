package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemGridToolBinding
import com.example.otter.model.ToolItem

class ToolsAdapter(private val toolList: List<ToolItem>) : RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGridToolBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGridToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = toolList[position]
        holder.binding.ivToolIcon.setImageResource(tool.iconResId)
        holder.binding.tvToolName.text = tool.name
    }

    override fun getItemCount(): Int {
        return toolList.size
    }
}