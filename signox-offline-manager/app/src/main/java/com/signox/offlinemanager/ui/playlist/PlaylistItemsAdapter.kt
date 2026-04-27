package com.signox.offlinemanager.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.data.model.PlaylistItem
import com.signox.offlinemanager.databinding.ItemPlaylistMediaBinding
import com.signox.offlinemanager.ui.playlist.PlaylistEditorViewModel.PlaylistItemWithMedia
import java.io.File
import java.util.concurrent.TimeUnit

class PlaylistItemsAdapter(
    private val onRemoveClick: (PlaylistItem) -> Unit,
    private val onDurationChanged: (PlaylistItem, Long) -> Unit
) : ListAdapter<PlaylistItemWithMedia, PlaylistItemsAdapter.PlaylistItemViewHolder>(PlaylistItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistItemViewHolder {
        val binding = ItemPlaylistMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistItemViewHolder(
        private val binding: ItemPlaylistMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: PlaylistItemWithMedia? = null

        init {
            // Set up duration change listener once
            binding.etCustomDuration.addTextChangedListener { text ->
                val newDurationSeconds = text.toString().toLongOrNull()
                if (newDurationSeconds != null && newDurationSeconds > 0) {
                    currentItem?.let { itemWithMedia ->
                        // Convert seconds to milliseconds
                        onDurationChanged(itemWithMedia.playlistItem, newDurationSeconds * 1000)
                    }
                }
            }
        }

        fun bind(itemWithMedia: PlaylistItemWithMedia) {
            currentItem = itemWithMedia
            val playlistItem = itemWithMedia.playlistItem
            val media = itemWithMedia.media
            
            binding.apply {
                tvMediaName.text = media.name
                tvPosition.text = (playlistItem.position + 1).toString()
                
                // Format duration display - show original media duration
                val originalDurationText = if (media.duration > 0) {
                    "Original Duration: ${formatDuration(media.duration)}"
                } else {
                    "Media file"
                }
                tvDuration.text = originalDurationText

                // Set custom duration in seconds (avoid triggering listener)
                val durationInSeconds = playlistItem.duration / 1000
                etCustomDuration.setText(durationInSeconds.toString())

                // Load thumbnail
                loadThumbnail(media)

                btnRemove.setOnClickListener {
                    onRemoveClick(playlistItem)
                }
            }
        }

        private fun loadThumbnail(media: com.signox.offlinemanager.data.model.Media) {
            when (media.type) {
                MediaType.IMAGE -> {
                    Glide.with(binding.ivThumbnail.context)
                        .load(File(media.filePath))
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .centerCrop()
                        .into(binding.ivThumbnail)
                }
                MediaType.VIDEO -> {
                    if (!media.thumbnailPath.isNullOrBlank() && File(media.thumbnailPath).exists()) {
                        Glide.with(binding.ivThumbnail.context)
                            .load(File(media.thumbnailPath))
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .centerCrop()
                            .into(binding.ivThumbnail)
                    } else {
                        binding.ivThumbnail.setImageResource(R.drawable.ic_video)
                    }
                }
                MediaType.AUDIO -> {
                    binding.ivThumbnail.setImageResource(R.drawable.ic_audio)
                }
            }
        }

        private fun formatDuration(durationMs: Long): String {
            if (durationMs == 0L) return "0:00"
            
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    private class PlaylistItemDiffCallback : DiffUtil.ItemCallback<PlaylistItemWithMedia>() {
        override fun areItemsTheSame(
            oldItem: PlaylistItemWithMedia,
            newItem: PlaylistItemWithMedia
        ): Boolean {
            return oldItem.playlistItem.playlistId == newItem.playlistItem.playlistId &&
                   oldItem.playlistItem.mediaId == newItem.playlistItem.mediaId
        }

        override fun areContentsTheSame(
            oldItem: PlaylistItemWithMedia,
            newItem: PlaylistItemWithMedia
        ): Boolean {
            return oldItem == newItem
        }
    }
}