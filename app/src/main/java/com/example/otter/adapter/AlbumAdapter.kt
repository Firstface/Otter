package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemAlbumBinding
import com.example.otter.model.AlbumItem

/**
 * 相册列表适配器，用于在RecyclerView中显示相册数据
 * @param onItemClick 点击相册项时的回调函数，参数为点击位置
 */
class AlbumAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<AlbumItem, AlbumAdapter.ViewHolder>(AlbumDiffCallback()) {

    /**
     * 创建视图持有者，初始化item视图绑定
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    /**
     * 绑定数据到视图持有者
     * @param holder 视图持有者
     * @param position 当前数据项的位置
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvAlbumName.text = item.name      // 设置相册名称
        holder.itemView.isSelected = item.isSelected     // 更新选中状态
        holder.itemView.setOnClickListener {             // 设置点击监听器
            onItemClick(position)
        }
    }

    /**
     * 视图持有者，持有相册项的视图绑定引用
     */
    class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * 相册项差异比较回调，用于优化列表更新
     */
    class AlbumDiffCallback : DiffUtil.ItemCallback<AlbumItem>() {
        /**
         * 判断是否为同一个相册项（通过名称判断）
         */
        override fun areItemsTheSame(oldItem: AlbumItem, newItem: AlbumItem): Boolean {
            return oldItem.name == newItem.name
        }

        /**
         * 判断相册项内容是否相同（自动比较数据类字段）
         */
        override fun areContentsTheSame(oldItem: AlbumItem, newItem: AlbumItem): Boolean {
            return oldItem == newItem
        }
    }
}