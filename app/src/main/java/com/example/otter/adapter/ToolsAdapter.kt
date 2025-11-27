package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemGridToolBinding
import com.example.otter.model.ToolItem

// 工具列表适配器，用于在RecyclerView中显示工具项网格
class ToolsAdapter(private val toolList: List<ToolItem>) : RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

    // ViewHolder用于绑定列表项的视图组件
    inner class ViewHolder(val binding: ItemGridToolBinding) : RecyclerView.ViewHolder(binding.root)

    // 创建新ViewHolder时调用，用于膨胀列表项布局
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGridToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // 绑定数据到ViewHolder，设置图标和工具名称
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = toolList[position]
        holder.binding.ivToolIcon.setImageResource(tool.iconResId) // 设置工具图标
        holder.binding.tvToolName.text = tool.name                // 设置工具名称
    }

    // 返回工具列表的总项数
    override fun getItemCount(): Int {
        return toolList.size
    }
}
