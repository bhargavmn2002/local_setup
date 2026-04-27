package com.signox.dashboard.ui.display

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.databinding.ItemDisplayCardBinding
import java.text.SimpleDateFormat
import java.util.*

class DisplayAdapter(
    private val onDisplayClick: (Display) -> Unit
) : ListAdapter<Display, DisplayAdapter.DisplayViewHolder>(DisplayDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        val binding = ItemDisplayCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DisplayViewHolder(binding, onDisplayClick)
    }
    
    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DisplayViewHolder(
        private val binding: ItemDisplayCardBinding,
        private val onDisplayClick: (Display) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(display: Display) {
            binding.apply {
                // Display name
                tvDisplayName.text = display.name
                
                // Status indicator
                val statusColor = when (display.status.uppercase()) {
                    "ONLINE" -> R.color.status_online
                    "OFFLINE" -> R.color.status_offline
                    "PAIRING" -> R.color.status_pairing
                    else -> R.color.status_error
                }
                viewStatusIndicator.setBackgroundColor(
                    ContextCompat.getColor(root.context, statusColor)
                )
                tvStatus.text = display.status
                tvStatus.setTextColor(
                    ContextCompat.getColor(root.context, statusColor)
                )
                
                // Location
                if (!display.location.isNullOrEmpty()) {
                    tvLocation.text = display.location
                } else {
                    tvLocation.text = "No location set"
                }
                
                // Last seen
                if (display.lastSeenAt != null) {
                    tvLastSeen.text = "Last seen: ${formatDate(display.lastSeenAt)}"
                } else {
                    tvLastSeen.text = "Never seen"
                }
                
                // Assigned content
                val contentText = when {
                    display.activeSchedule != null -> {
                        "📅 ${display.activeSchedule.contentName ?: "Scheduled content"}"
                    }
                    display.playlist != null -> {
                        "🎵 ${display.playlist.name}"
                    }
                    display.layout != null -> {
                        "🎨 ${display.layout.name}"
                    }
                    else -> {
                        "No content assigned"
                    }
                }
                tvContent.text = contentText
                
                // Click listener
                root.setOnClickListener {
                    onDisplayClick(display)
                }
            }
        }
        
        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                
                val now = Date()
                val diff = now.time - (date?.time ?: 0)
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                
                when {
                    seconds < 60 -> "Just now"
                    minutes < 60 -> "$minutes min ago"
                    hours < 24 -> "$hours hours ago"
                    days < 7 -> "$days days ago"
                    else -> {
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        outputFormat.format(date!!)
                    }
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }
    
    class DisplayDiffCallback : DiffUtil.ItemCallback<Display>() {
        override fun areItemsTheSame(oldItem: Display, newItem: Display): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Display, newItem: Display): Boolean {
            return oldItem == newItem
        }
    }
}
