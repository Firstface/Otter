package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otter.databinding.ItemSelectedPhotoBinding
import com.example.otter.model.PhotoItem

/**
 * 已选择照片项适配器，用于显示已选择的照片列表
 * @param onPhotoClick 点击回调函数，参数为点击项的照片项
 */
class SelectedPhotoAdapter(
    private val onPhotoClick: (PhotoItem) -> Unit
) : ListAdapter<PhotoItem, SelectedPhotoAdapter.ViewHolder>(DiffCallback()) {

        /**
         * ViewHolder，包含已选择照片项布局的视图绑定对象
         */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
        /**
         * 绑定数据到ViewHolder的视图
         * @param holder 要绑定数据的ViewHolder
         * @param position 数据项在列表中的位置
         */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
        /**
         * ViewHolder，包含已选择照片项布局的视图绑定对象
         * @param binding 已选择照片项布局的视图绑定对象
         */
    inner class ViewHolder(private val binding: ItemSelectedPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhotoClick(getItem(position))
                }
            }
        }
        /**
         * 绑定照片项数据到ViewHolder的视图
         * @param photo 要绑定的照片项数据
         */
        fun bind(photo: PhotoItem) {
            Glide.with(binding.ivPhoto.context)
                .load(photo.uri)
                .into(binding.ivPhoto)
        }
    }
        /**
         * DiffUtil.ItemCallback，用于计算列表项的差异
         */
    private class DiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
        override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem == newItem
        }
    }
}