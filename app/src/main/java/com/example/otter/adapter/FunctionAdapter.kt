package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemFunctionBinding
import com.example.otter.model.FunctionItem

class FunctionAdapter(
    private val functionList: List<FunctionItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFunctionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = functionList[position]
        holder.binding.tvFunctionName.text = item.name
        holder.itemView.isSelected = item.isSelected

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return functionList.size
    }
}