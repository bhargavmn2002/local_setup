package com.signox.offlinemanager.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.databinding.ItemLayoutMediaBinding

class LayoutMediaAdapter(
    private val onMediaClick: (Media) -> Unit
) : ListAdapter<Media, LayoutMediaAdapter.MediaViewHolder>(MediaDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemLayoutMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MediaViewHolder(
        private val binding: ItemLayoutMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(media: Media) {
            binding.apply {
                textMediaName.text = media.name
                
                // Load thumbnail
                Glide.with(imageMediaThumbnail.context)
                    .load(media.filePath)
                    .placeholder(R.drawable.ic_media)
                    .error(R.drawable.ic_media)
                    .centerCrop()
                    .into(imageMediaThumbnail)
                
                // Set type icon
                val typeIcon = when (media.type) {
                    MediaType.IMAGE -> R.drawable.ic_image
                    MediaType.VIDEO -> R.drawable.ic_video
                    MediaType.AUDIO -> R.drawable.ic_audio
                }
                imageMediaType.setImageResource(typeIcon)
                
                root.setOnClickListener { onMediaClick(media) }
            }
        }
    }
    
    class MediaDiffCallback : DiffUtil.ItemCallback<Media>() {
        override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem == newItem
        }
    }
}