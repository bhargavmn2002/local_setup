package com.signox.offlinemanager.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.R
import com.signox.offlinemanager.databinding.ItemPlaylistBinding
import com.signox.offlinemanager.ui.playlist.PlaylistViewModel.PlaylistWithDetails
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PlaylistAdapter(
    private val onPlaylistClick: (PlaylistWithDetails) -> Unit,
    private val onEditClick: (PlaylistWithDetails) -> Unit,
    private val onDeleteClick: (PlaylistWithDetails) -> Unit,
    private val onPreviewClick: (PlaylistWithDetails) -> Unit
) : ListAdapter<PlaylistWithDetails, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(
        private val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlistWithDetails: PlaylistWithDetails) {
            val playlist = playlistWithDetails.playlist
            
            binding.apply {
                tvPlaylistName.text = playlist.name
                tvPlaylistDescription.text = playlist.description ?: "No description"
                tvPlaylistDescription.visibility = if (playlist.description.isNullOrBlank()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                
                tvItemCount.text = "${playlistWithDetails.itemCount} items"
                tvDuration.text = formatDuration(playlistWithDetails.totalDuration)

                root.setOnClickListener {
                    onPlaylistClick(playlistWithDetails)
                }

                btnMenu.setOnClickListener { view ->
                    showPopupMenu(view, playlistWithDetails)
                }
            }
        }

        private fun showPopupMenu(view: View, playlistWithDetails: PlaylistWithDetails) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_playlist_options, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(playlistWithDetails)
                        true
                    }
                    R.id.action_preview -> {
                        onPreviewClick(playlistWithDetails)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(playlistWithDetails)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun formatDuration(durationMs: Long): String {
            if (durationMs == 0L) return "0:00"
            
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    private class PlaylistDiffCallback : DiffUtil.ItemCallback<PlaylistWithDetails>() {
        override fun areItemsTheSame(
            oldItem: PlaylistWithDetails,
            newItem: PlaylistWithDetails
        ): Boolean {
            return oldItem.playlist.id == newItem.playlist.id
        }

        override fun areContentsTheSame(
            oldItem: PlaylistWithDetails,
            newItem: PlaylistWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}