package com.signox.dashboard.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.databinding.ItemDisplayCheckboxBinding

class DisplayCheckboxAdapter(
    private val preSelectedIds: List<String>,
    private val onDisplaySelected: (Display, Boolean) -> Unit
) : ListAdapter<Display, DisplayCheckboxAdapter.DisplayViewHolder>(DisplayDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        val binding = ItemDisplayCheckboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DisplayViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DisplayViewHolder(
        private val binding: ItemDisplayCheckboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(display: Display) {
            binding.displayCheckbox.text = display.name
            binding.displayCheckbox.isChecked = preSelectedIds.contains(display.id)
            binding.displayStatus.text = display.status
            
            binding.displayCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onDisplaySelected(display, isChecked)
            }
        }
    }
    
    private class DisplayDiffCallback : DiffUtil.ItemCallback<Display>() {
        override fun areItemsTheSame(oldItem: Display, newItem: Display): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Display, newItem: Display): Boolean {
            return oldItem == newItem
        }
    }
}
