package com.example.otter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.otter.databinding.ItemAlbumBinding
import com.example.otter.model.AlbumItem

class AlbumAdapter(
    private val albumList: List<AlbumItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = albumList[position]
        holder.binding.tvAlbumName.text = item.name
        holder.itemView.isSelected = item.isSelected

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return albumList.size
    }
}