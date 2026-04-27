package com.signox.dashboard.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.LayoutSection
import com.signox.dashboard.databinding.ItemSectionCardBinding

class SectionAdapter(
    private val onSectionClick: (LayoutSection) -> Unit
) : ListAdapter<LayoutSection, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSectionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class SectionViewHolder(
        private val binding: ItemSectionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(section: LayoutSection) {
            binding.apply {
                sectionName.text = section.name
                sectionOrder.text = "Order: ${section.order + 1}"
                
                val itemCount = section.items?.size ?: 0
                sectionInfo.text = "$itemCount items • ${section.width.toInt()}x${section.height.toInt()}%"
                
                sectionPosition.text = "Position: (${section.x.toInt()}%, ${section.y.toInt()}%)"
                
                root.setOnClickListener { onSectionClick(section) }
            }
        }
    }
    
    private class SectionDiffCallback : DiffUtil.ItemCallback<LayoutSection>() {
        override fun areItemsTheSame(oldItem: LayoutSection, newItem: LayoutSection): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LayoutSection, newItem: LayoutSection): Boolean {
            return oldItem == newItem
        }
    }
}
