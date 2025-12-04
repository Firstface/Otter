package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.R
import com.example.otter.model.EditingToolType

/**
 * RecyclerView适配器，用于显示编辑工具列表
 * @param tools 编辑工具类型列表
 * @param onToolSelected 工具点击回调函数
 */
class EditingToolAdapter(
    private val tools: List<EditingToolType>,
    private val onToolSelected: (EditingToolType) -> Unit
) : RecyclerView.Adapter<EditingToolAdapter.ToolViewHolder>() {

    /**
     * ViewHolder类，用于保存列表项视图的引用
     * @param itemView 列表项视图
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_editing_tool, parent, false)
        return ToolViewHolder(view)
    }

    /**
     * 将数据绑定到ViewHolder
     * @param holder ViewHolder实例
     * @param position 数据项位置
     */
    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.bind(tool)
        holder.itemView.setOnClickListener { onToolSelected(tool) }
    }

        /**
         * 获取数据项总数
         * @return 数据项数量
         */
        override fun getItemCount(): Int = tools.size

        /**
         * ViewHolder类，用于保存列表项视图的引用
         * @param itemView 列表项视图
         */
        class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.iv_tool_icon)
        private val name: TextView = itemView.findViewById(R.id.tv_tool_name)
            /**
         * 绑定编辑工具数据到视图
         * @param tool 编辑工具类型
         */
        fun bind(tool: EditingToolType) {
            name.text = itemView.context.getString(tool.titleRes)
        }

                /**
         * 设置工具名称文本颜色
         * @param color 文本颜色
         */
        fun setTextColor(@ColorInt color: Int) {
            name.setTextColor(color)
        }
    }
}