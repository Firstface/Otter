package com.example.otter.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otter.databinding.ItemEditingThumbnailBinding

/**
 * RecyclerView适配器，用于显示编辑缩略图列表
 * @param photos 图片URI列表
 * @param onThumbnailClick 缩略图点击回调函数
 */
class EditingThumbnailAdapter(
    private val photos: List<Uri>,
    private val onThumbnailClick: (Uri) -> Unit
) : RecyclerView.Adapter<EditingThumbnailAdapter.ViewHolder>() {

    // 创建ViewHolder实例
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditingThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // 将数据绑定到ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    // 获取数据项总数
    override fun getItemCount(): Int = photos.size

    /**
     * ViewHolder类，用于保存列表项视图的引用
     * @param binding 数据绑定对象
     */
    inner class ViewHolder(private val binding: ItemEditingThumbnailBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 设置缩略图点击事件
            binding.root.setOnClickListener {
                onThumbnailClick(photos[bindingAdapterPosition])
            }
        }

        /**
         * 绑定图片数据到视图
         * @param uri 图片资源URI
         */
        fun bind(uri: Uri) {
            // 使用Glide加载图片到ImageView
            Glide.with(binding.ivThumbnail.context)
                .load(uri)
                .into(binding.ivThumbnail)
        }
    }
}