package com.signox.offlinemanager.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.databinding.ItemMediaBinding
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class MediaAdapter(
    private val onPreviewClick: (Media) -> Unit,
    private val onDeleteClick: (Media) -> Unit
) : ListAdapter<Media, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
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
        private val binding: ItemMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(media: Media) {
            binding.apply {
                // Set media name
                tvMediaName.text = media.name
                
                // Set file size
                tvFileSize.text = formatFileSize(media.fileSize)
                
                // Set media type icon
                val typeIcon = when (media.type) {
                    MediaType.IMAGE -> R.drawable.ic_image
                    MediaType.VIDEO -> R.drawable.ic_video
                    MediaType.AUDIO -> R.drawable.ic_audio
                }
                ivMediaType.setImageResource(typeIcon)
                
                // Show duration for videos and audio
                if (media.type == MediaType.VIDEO || media.type == MediaType.AUDIO) {
                    tvDuration.visibility = android.view.View.VISIBLE
                    tvDuration.text = formatDuration(media.duration)
                } else {
                    tvDuration.visibility = android.view.View.GONE
                }
                
                // Load thumbnail
                loadThumbnail(media)
                
                // Set click listeners
                btnPreview.setOnClickListener { onPreviewClick(media) }
                btnDelete.setOnClickListener { onDeleteClick(media) }
                
                // Set item click listener for preview
                root.setOnClickListener { onPreviewClick(media) }
            }
        }
        
        private fun loadThumbnail(media: Media) {
            val file = File(media.filePath)
            if (!file.exists()) {
                // File doesn't exist, show placeholder
                binding.ivThumbnail.setImageResource(R.drawable.ic_media)
                return
            }
            
            when (media.type) {
                MediaType.IMAGE -> {
                    // Load image directly
                    Glide.with(binding.ivThumbnail.context)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(binding.ivThumbnail)
                }
                MediaType.VIDEO -> {
                    // Load video thumbnail
                    if (media.thumbnailPath != null && File(media.thumbnailPath).exists()) {
                        Glide.with(binding.ivThumbnail.context)
                            .load(File(media.thumbnailPath))
                            .centerCrop()
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .into(binding.ivThumbnail)
                    } else {
                        // Generate thumbnail from video
                        Glide.with(binding.ivThumbnail.context)
                            .load(file)
                            .centerCrop()
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .into(binding.ivThumbnail)
                    }
                }
                MediaType.AUDIO -> {
                    // Show audio icon
                    binding.ivThumbnail.setImageResource(R.drawable.ic_audio)
                }
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            
            return DecimalFormat("#,##0.#").format(
                bytes / 1024.0.pow(digitGroups.toDouble())
            ) + " " + units[digitGroups]
        }
        
        private fun formatDuration(milliseconds: Long): String {
            val seconds = milliseconds / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            } else {
                String.format("%d:%02d", minutes, seconds % 60)
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