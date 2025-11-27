package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemFunctionBinding
import com.example.otter.model.FunctionItem

/**
 * 功能项适配器，用于显示功能列表
 * @param functionList 功能项数据列表
 * @param onItemClick 点击回调函数，参数为点击项的位置
 */
class FunctionAdapter(
    /**
     * 功能项数据列表
     */
    private val functionList: List<FunctionItem>,
    /**
     * 点击回调函数，参数为点击项的位置
     */
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {
    /**
     * ViewHolder，包含功能项布局的视图绑定对象
     */
    inner class ViewHolder(val binding: ItemFunctionBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 功能项名称文本视图
         */
        val tvFunctionName = binding.tvFunctionName
    }

    /**
     * 创建ViewHolder，初始化列表项布局
     * @param parent 父视图组，通常是RecyclerView
     * @param viewType 视图类型，默认0
     * @return 包含视图绑定对象的ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        /**
         * 功能项布局的视图绑定对象
         */
        val binding = ItemFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    /**
     * 绑定数据到ViewHolder的视图
     * @param holder 要绑定数据的ViewHolder
     * @param position 数据项在列表中的位置
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = functionList[position]
        holder.binding.tvFunctionName.text = item.type.displayName
        holder.itemView.isSelected = item.isSelected

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }
    /**
     * 返回功能项数据列表的大小
     * @return 功能项数据列表的大小
     */
    override fun getItemCount(): Int {
        return functionList.size
    }
}