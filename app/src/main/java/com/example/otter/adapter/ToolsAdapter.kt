package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemGridToolBinding
import com.example.otter.model.ToolItem

/**
 * 工具列表适配器，用于在RecyclerView中显示工具项网格
 * @param toolList 工具项数据列表
 * @param onItemClick 点击回调函数，参数为点击项的工具项
 */
class ToolsAdapter(
    private val toolList: List<ToolItem>,
    private val onItemClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

        /**
         * ViewHolder，包含工具项布局的视图绑定对象
         */
    class ViewHolder(val binding: ItemGridToolBinding) : RecyclerView.ViewHolder(binding.root)

        /**
         * 创建新ViewHolder时调用，用于膨胀列表项布局
         * @param parent 父视图组
         * @param viewType 视图类型
         * @return 新创建的ViewHolder
         */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGridToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

        /**
         * 绑定数据到ViewHolder的视图
         * @param holder 要绑定数据的ViewHolder
         * @param position 数据项在列表中的位置
         */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = toolList[position]
        holder.binding.ivToolIcon.setImageResource(tool.type.iconResId)
        holder.binding.tvToolName.text = tool.type.displayName
        holder.itemView.setOnClickListener {
            onItemClick(tool)
        }
    }


        /**
         * 返回工具列表的总项数
         * @return 工具项数据列表的大小
         */
    override fun getItemCount(): Int {
        return toolList.size
    }
}
