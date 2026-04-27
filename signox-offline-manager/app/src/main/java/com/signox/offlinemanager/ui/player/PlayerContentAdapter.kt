package com.signox.offlinemanager.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.databinding.ItemPlayerContentBinding
import java.text.SimpleDateFormat
import java.util.*

class PlayerContentAdapter(
    private val onItemClick: (PlayerContentItem) -> Unit
) : ListAdapter<PlayerContentItem, PlayerContentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlayerContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPlayerContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlayerContentItem) {
            binding.apply {
                textContentName.text = item.name
                textContentDescription.text = item.description
                textContentType.text = item.type.name.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
                
                // Format duration for playlists
                textContentDuration.text = if (item.type == PlayerContentType.PLAYLIST && item.duration > 0) {
                    formatDuration(item.duration)
                } else {
                    "Variable duration"
                }
                
                // Format creation date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textContentDate.text = dateFormat.format(Date(item.createdAt))
                
                // Set type-specific icon
                imageContentType.setImageResource(
                    if (item.type == PlayerContentType.PLAYLIST) {
                        com.signox.offlinemanager.R.drawable.ic_playlist
                    } else {
                        com.signox.offlinemanager.R.drawable.ic_layout
                    }
                )
                
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
        
        private fun formatDuration(durationMs: Long): String {
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            return when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
                minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
                else -> "${seconds}s"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PlayerContentItem>() {
        override fun areItemsTheSame(oldItem: PlayerContentItem, newItem: PlayerContentItem): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: PlayerContentItem, newItem: PlayerContentItem): Boolean {
            return oldItem == newItem
        }
    }
}