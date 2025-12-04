package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otter.databinding.ItemPhotoBinding
import com.example.otter.model.PhotoItem


/**
 * 照片项适配器，用于显示照片列表
 * @param onPhotoClick 点击回调函数，参数为点击项的照片项
 */
class PhotoAdapter(
    /**
     * 点击回调函数，参数为点击项的照片项
     */
    private val onPhotoClick: (PhotoItem) -> Unit
) : ListAdapter<PhotoItem, PhotoAdapter.ViewHolder>(PhotoDiffCallback()) {

        /**
         * ViewHolder，包含照片项布局的视图绑定对象
         */
        inner class ViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
            /**
             * 照片项图像视图
             */
            val ivPhoto = binding.ivPhoto
            /**
             * 照片项选择覆盖层视图
             */
            val selectionOverlay = binding.selectionOverlay
            /**
             * 绑定照片项数据到视图
             * @param photo 照片项数据
             */
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhotoClick(getItem(position))
                }
            }
        }

                /**
             * 绑定照片项数据到视图
             * @param photo 照片项数据
             */
        fun bind(photo: PhotoItem) {
            Glide.with(itemView.context)
                .load(photo.uri)
                .into(binding.ivPhoto)

            binding.selectionOverlay.visibility = if (photo.isSelected) View.VISIBLE else View.GONE
        }
    }

        /**
         * 创建ViewHolder，初始化列表项布局
         * @param parent 父视图组，通常是RecyclerView
         * @param viewType 视图类型，默认0
         * @return 包含视图绑定对象的ViewHolder
         */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
}
        /**
         * 照片项差异回调，用于计算列表项的差异
         */
class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
        /**
         * 判断两个照片项是否表示相同的项
         * @param oldItem 旧照片项
         * @param newItem 新照片项
         * @return 如果表示相同项，则返回true，否则返回false
         */
    override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem.uri == newItem.uri
    }
        /**
         * 判断两个照片项的内容是否相同
         * @param oldItem 旧照片项
         * @param newItem 新照片项
         * @return 如果内容相同，则返回true，否则返回false
         */
    override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem == newItem
    }
}