package com.signox.dashboard.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.Layout
import com.signox.dashboard.databinding.ItemLayoutCardBinding

class LayoutAdapter(
    private val onLayoutClick: (Layout) -> Unit,
    private val onDeleteClick: (Layout) -> Unit
) : ListAdapter<Layout, LayoutAdapter.LayoutViewHolder>(LayoutDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutViewHolder {
        val binding = ItemLayoutCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LayoutViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class LayoutViewHolder(
        private val binding: ItemLayoutCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(layout: Layout) {
            binding.apply {
                layoutName.text = layout.name
                layoutDescription.text = layout.description ?: "No description"
                
                // Display layout info
                val sectionCount = layout.count?.sections ?: layout.sections?.size ?: 0
                val displayCount = layout.count?.displays ?: 0
                layoutInfo.text = "$sectionCount sections • $displayCount displays"
                
                // Display dimensions and orientation
                layoutDimensions.text = "${layout.width}x${layout.height} • ${layout.orientation}"
                
                // Status indicator
                statusIndicator.setBackgroundColor(
                    if (layout.isActive) {
                        android.graphics.Color.parseColor("#4CAF50") // Green
                    } else {
                        android.graphics.Color.parseColor("#9E9E9E") // Gray
                    }
                )
                
                // Click listeners
                root.setOnClickListener { onLayoutClick(layout) }
                deleteButton.setOnClickListener { onDeleteClick(layout) }
            }
        }
    }
    
    private class LayoutDiffCallback : DiffUtil.ItemCallback<Layout>() {
        override fun areItemsTheSame(oldItem: Layout, newItem: Layout): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Layout, newItem: Layout): Boolean {
            return oldItem == newItem
        }
    }
}
