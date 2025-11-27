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

class PhotoAdapter(
    private val onPhotoClick: (PhotoItem) -> Unit
) : ListAdapter<PhotoItem, PhotoAdapter.ViewHolder>(PhotoDiffCallback()) {

    inner class ViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhotoClick(getItem(position))
                }
            }
        }

        fun bind(photo: PhotoItem) {
            Glide.with(itemView.context)
                .load(photo.uri)
                .into(binding.ivPhoto)

            binding.selectionOverlay.visibility = if (photo.isSelected) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
    override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem == newItem
    }
}