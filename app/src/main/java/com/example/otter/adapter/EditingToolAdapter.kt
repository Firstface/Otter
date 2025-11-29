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

class EditingToolAdapter(
    private val tools: List<EditingToolType>,
    private val onToolSelected: (EditingToolType) -> Unit
) : RecyclerView.Adapter<EditingToolAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_editing_tool, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.bind(tool)
        holder.itemView.setOnClickListener { onToolSelected(tool) }
    }

    override fun getItemCount(): Int = tools.size

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.iv_tool_icon)
        private val name: TextView = itemView.findViewById(R.id.tv_tool_name)

        fun bind(tool: EditingToolType) {
            name.text = itemView.context.getString(tool.titleRes)
        }

        fun setTextColor(@ColorInt color: Int) {
            name.setTextColor(color)
        }
    }
}