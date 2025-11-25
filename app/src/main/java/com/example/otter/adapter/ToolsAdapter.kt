package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.R
import com.example.otter.model.ToolItem

class ToolsAdapter(private val toolList: List<ToolItem>) :
    RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        // 加载你的子项布局 item_grid_tool.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grid_tool, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val item = toolList[position]
        holder.tvName.text = item.name
        holder.ivIcon.setImageResource(item.iconResId)

        // 如果需要点击事件，可以在这里写:
        // holder.itemView.setOnClickListener { ... }
    }

    override fun getItemCount(): Int = toolList.size

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_tool_icon)
        val tvName: TextView = itemView.findViewById(R.id.tv_tool_name)
    }
}