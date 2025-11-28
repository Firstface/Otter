package com.example.otter.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otter.databinding.ItemEditingThumbnailBinding

class EditingThumbnailAdapter(
    private val photos: List<Uri>,
    private val onThumbnailClick: (Uri) -> Unit
) : RecyclerView.Adapter<EditingThumbnailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditingThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    inner class ViewHolder(private val binding: ItemEditingThumbnailBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onThumbnailClick(photos[bindingAdapterPosition])
            }
        }

        fun bind(uri: Uri) {
            Glide.with(binding.ivThumbnail.context)
                .load(uri)
                .into(binding.ivThumbnail)
        }
    }
}