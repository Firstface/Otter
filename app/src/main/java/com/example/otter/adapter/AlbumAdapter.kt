package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemAlbumBinding
import com.example.otter.model.AlbumItem

// 相册列表适配器，用于在 RecyclerView 中显示相册项
class AlbumAdapter(
    // 相册数据源列表
    private val albumList: List<AlbumItem>,
    // 点击项的监听回调，参数为被点击项的位置
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    // ViewHolder 类持有视图绑定引用
    inner class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    // 创建新 ViewHolder 实例时调用（父容器会自动调用）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // 将数据绑定到指定位置的 ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = albumList[position]
        // 设置相册名称
        holder.binding.tvAlbumName.text = item.name
        // 设置选中状态
        holder.itemView.isSelected = item.isSelected

        // 处理项点击事件
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    // 返回数据源的总项数
    override fun getItemCount(): Int {
        return albumList.size
    }
}