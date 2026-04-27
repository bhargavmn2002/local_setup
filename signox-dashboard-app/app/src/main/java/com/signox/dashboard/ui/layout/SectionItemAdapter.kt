package com.signox.dashboard.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.CreateSectionItemRequest
import com.signox.dashboard.databinding.ItemSectionItemBinding

class SectionItemAdapter(
    private val onRemoveClick: (Int) -> Unit,
    private val onDurationChange: (Int, Int) -> Unit
) : ListAdapter<CreateSectionItemRequest, SectionItemAdapter.ItemViewHolder>(ItemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemSectionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    inner class ItemViewHolder(
        private val binding: ItemSectionItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: CreateSectionItemRequest, position: Int) {
            binding.apply {
                itemOrder.text = "${position + 1}"
                itemMediaId.text = "Media: ${item.mediaId.take(8)}..."
                itemResizeMode.text = "Resize: ${item.resizeMode}"
                
                // Duration input
                durationEditText.setText(item.duration?.toString() ?: "10")
                durationEditText.addTextChangedListener { text ->
                    val duration = text?.toString()?.toIntOrNull() ?: 10
                    onDurationChange(position, duration)
                }
                
                // Remove button
                removeButton.setOnClickListener {
                    onRemoveClick(position)
                }
            }
        }
    }
    
    private class ItemDiffCallback : DiffUtil.ItemCallback<CreateSectionItemRequest>() {
        override fun areItemsTheSame(
            oldItem: CreateSectionItemRequest,
            newItem: CreateSectionItemRequest
        ): Boolean {
            return oldItem.mediaId == newItem.mediaId && oldItem.order == newItem.order
        }
        
        override fun areContentsTheSame(
            oldItem: CreateSectionItemRequest,
            newItem: CreateSectionItemRequest
        ): Boolean {
            return oldItem == newItem
        }
    }
}
