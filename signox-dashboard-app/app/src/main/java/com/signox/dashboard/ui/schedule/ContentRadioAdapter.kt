package com.signox.dashboard.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.databinding.ItemContentRadioBinding

class ContentRadioAdapter(
    private val onContentSelected: (id: String, name: String) -> Unit
) : ListAdapter<ContentItem, ContentRadioAdapter.ContentViewHolder>(ContentDiffCallback()) {
    
    private var selectedPosition = -1
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val binding = ItemContentRadioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }
    
    inner class ContentViewHolder(
        private val binding: ItemContentRadioBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ContentItem, isSelected: Boolean) {
            binding.contentRadio.text = item.name
            binding.contentRadio.isChecked = isSelected
            
            binding.contentRadio.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                
                onContentSelected(item.id, item.name)
            }
        }
    }
    
    private class ContentDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem == newItem
        }
    }
}
