package com.signox.dashboard.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.PlaybackLog
import com.signox.dashboard.databinding.ItemPlaybackLogBinding
import java.text.SimpleDateFormat
import java.util.*

class PlaybackLogAdapter : ListAdapter<PlaybackLog, PlaybackLogAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaybackLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemPlaybackLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        fun bind(log: PlaybackLog) {
            binding.apply {
                tvContentName.text = log.contentName
                tvDisplayName.text = log.displayName ?: "Unknown Display"
                tvContentType.text = log.contentType.uppercase()
                
                // Format date
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(log.playedAt)
                    tvPlayedAt.text = date?.let { dateFormat.format(it) } ?: log.playedAt
                } catch (e: Exception) {
                    tvPlayedAt.text = log.playedAt
                }
                
                // Duration
                val minutes = log.duration / 60
                val seconds = log.duration % 60
                tvDuration.text = String.format("%d:%02d", minutes, seconds)
                
                // Status
                tvStatus.text = if (log.completed) "Completed" else "Incomplete"
                tvStatus.setTextColor(
                    if (log.completed) {
                        android.graphics.Color.parseColor("#4CAF50")
                    } else {
                        android.graphics.Color.parseColor("#FF9800")
                    }
                )
                
                // Verified badge
                tvVerified.visibility = if (log.verified) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<PlaybackLog>() {
        override fun areItemsTheSame(oldItem: PlaybackLog, newItem: PlaybackLog): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: PlaybackLog, newItem: PlaybackLog): Boolean {
            return oldItem == newItem
        }
    }
}
