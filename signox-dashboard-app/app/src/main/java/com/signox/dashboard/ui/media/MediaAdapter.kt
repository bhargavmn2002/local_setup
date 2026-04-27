package com.signox.dashboard.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.dashboard.BuildConfig
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.databinding.ItemMediaGridBinding

class MediaAdapter(
    private val onMediaClick: (Media) -> Unit,
    private val onMediaLongClick: (Media) -> Unit
) : ListAdapter<Media, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding, onMediaClick, onMediaLongClick)
    }
    
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class MediaViewHolder(
        private val binding: ItemMediaGridBinding,
        private val onMediaClick: (Media) -> Unit,
        private val onMediaLongClick: (Media) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(media: Media) {
            binding.apply {
                // Set media name
                tvMediaName.text = media.originalName
                
                // Set media type and size
                val typeText = if (media.isImage) "Image" else "Video"
                tvMediaInfo.text = "$typeText • ${media.formattedSize}"
                
                // Set duration for videos
                if (media.isVideo && media.formattedDuration != null) {
                    tvDuration.text = media.formattedDuration
                    tvDuration.visibility = android.view.View.VISIBLE
                } else {
                    tvDuration.visibility = android.view.View.GONE
                }
                
                // Load thumbnail
                val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/")
                val thumbnailUrl = when {
                    media.thumbnailUrl != null -> baseUrl + media.thumbnailUrl
                    media.previewUrl != null -> baseUrl + media.previewUrl
                    media.optimizedUrl != null -> baseUrl + media.optimizedUrl
                    media.isImage -> baseUrl + media.url
                    else -> null
                }
                
                if (thumbnailUrl != null) {
                    Glide.with(ivThumbnail.context)
                        .load(thumbnailUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(ivThumbnail)
                } else {
                    ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                
                // Set click listeners
                root.setOnClickListener { onMediaClick(media) }
                root.setOnLongClickListener {
                    onMediaLongClick(media)
                    true
                }
            }
        }
    }
    
    private class MediaDiffCallback : DiffUtil.ItemCallback<Media>() {
        override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem == newItem
        }
    }
}
