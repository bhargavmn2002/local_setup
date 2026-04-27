package com.signox.offlinemanager.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.data.model.LayoutZone
import com.signox.offlinemanager.databinding.ItemLayoutZoneBinding

class LayoutZoneAdapter(
    private val onZoneClick: (LayoutZone) -> Unit,
    private val onDeleteZone: (LayoutZone) -> Unit
) : ListAdapter<LayoutZone, LayoutZoneAdapter.ZoneViewHolder>(ZoneDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val binding = ItemLayoutZoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ZoneViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ZoneViewHolder(
        private val binding: ItemLayoutZoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(zone: LayoutZone) {
            binding.apply {
                textZoneName.text = zone.name
                textZonePosition.text = "Position: ${zone.x}, ${zone.y}"
                textZoneSize.text = "Size: ${zone.width} × ${zone.height}"
                
                // Show content information and instructions
                val hasContent = zone.mediaId != null || zone.playlistId != null
                
                if (hasContent) {
                    val contentText = when {
                        zone.mediaId != null -> "Media: ID ${zone.mediaId}"
                        zone.playlistId != null -> "Playlist: ID ${zone.playlistId}"
                        else -> "No content assigned"
                    }
                    textZoneContent.text = contentText
                    textZoneContent.visibility = android.view.View.VISIBLE
                    textZoneInstructions.visibility = android.view.View.GONE
                    
                    textZoneContent.setTextColor(
                        ContextCompat.getColor(root.context, com.signox.offlinemanager.R.color.primary)
                    )
                } else {
                    textZoneContent.text = "No content assigned"
                    textZoneContent.visibility = android.view.View.VISIBLE
                    textZoneInstructions.visibility = android.view.View.VISIBLE
                    
                    textZoneContent.setTextColor(
                        ContextCompat.getColor(root.context, com.signox.offlinemanager.R.color.text_secondary)
                    )
                }
                
                root.setOnClickListener { onZoneClick(zone) }
                
                buttonDeleteZone.setOnClickListener { onDeleteZone(zone) }
            }
        }
    }
    
    class ZoneDiffCallback : DiffUtil.ItemCallback<LayoutZone>() {
        override fun areItemsTheSame(oldItem: LayoutZone, newItem: LayoutZone): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LayoutZone, newItem: LayoutZone): Boolean {
            return oldItem == newItem
        }
    }
}