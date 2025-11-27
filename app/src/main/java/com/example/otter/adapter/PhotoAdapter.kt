package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otter.databinding.ItemPhotoBinding
import com.example.otter.model.PhotoItem

// 用于展示照片列表的RecyclerView适配器，基于ListAdapter实现高效更新
class PhotoAdapter : ListAdapter<PhotoItem, PhotoAdapter.ViewHolder>(PhotoDiffCallback()) {

    // ViewHolder类，持有视图绑定对象
    inner class ViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)

    // 创建新ViewHolder实例时调用，用于初始化视图绑定
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // 绑定数据到ViewHolder，使用Glide加载图片
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Glide.with(holder.itemView.context)
            .load(item.uri)           // 从PhotoItem加载图片URI
            .into(holder.binding.ivPhoto) // 显示到ImageView
    }
}

// 用于比较PhotoItem差异的回调类，继承自DiffUtil.ItemCallback
class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
    // 判断是否为同一个条目（通常使用唯一标识符比较）
    override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem.uri == newItem.uri  // 通过uri判断是否为同一张图片
    }

    // 判断内容是否相同（当areItemsTheSame返回true时才会调用）
    override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem == newItem  // 依赖PhotoItem的数据类特性进行全字段比较
    }
}
