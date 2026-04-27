package com.signox.dashboard.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.dashboard.R
import com.signox.dashboard.data.model.ContentStats
import com.signox.dashboard.databinding.ItemContentStatsBinding

class ContentStatsAdapter : ListAdapter<ContentStats, ContentStatsAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentStatsBinding.inflate(
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
        private val binding: ItemContentStatsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(stats: ContentStats) {
            binding.apply {
                tvContentName.text = stats.name
                tvContentType.text = stats.type.uppercase()
                tvPlayCount.text = "${stats.playCount} plays"
                
                val hours = stats.totalDuration / 3600
                val minutes = (stats.totalDuration % 3600) / 60
                tvTotalDuration.text = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }
                
                // Load thumbnail
                stats.thumbnail?.let { url ->
                    Glide.with(ivThumbnail.context)
                        .load(url)
                        .placeholder(R.drawable.ic_media)
                        .error(R.drawable.ic_media)
                        .centerCrop()
                        .into(ivThumbnail)
                }
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<ContentStats>() {
        override fun areItemsTheSame(oldItem: ContentStats, newItem: ContentStats): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ContentStats, newItem: ContentStats): Boolean {
            return oldItem == newItem
        }
    }
}
