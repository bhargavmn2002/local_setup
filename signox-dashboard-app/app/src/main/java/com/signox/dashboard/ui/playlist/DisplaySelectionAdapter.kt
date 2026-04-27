package com.signox.dashboard.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.databinding.ItemDisplayCheckboxBinding

class DisplaySelectionAdapter(
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<Display, DisplaySelectionAdapter.ViewHolder>(DisplayDiffCallback()) {
    
    private val selectedDisplayIds = mutableSetOf<String>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDisplayCheckboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun getSelectedDisplayIds(): Set<String> = selectedDisplayIds.toSet()
    
    fun clearSelection() {
        selectedDisplayIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedDisplayIds)
    }
    
    inner class ViewHolder(
        private val binding: ItemDisplayCheckboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(display: Display) {
            binding.apply {
                // Set display name on checkbox
                displayCheckbox.text = display.name
                
                // Status
                val isOnline = display.status == "online"
                val statusText = if (isOnline) "Online" else "Offline"
                val playlistInfo = if (display.playlist != null) {
                    " • ${display.playlist.name}"
                } else {
                    ""
                }
                displayStatus.text = "$statusText$playlistInfo"
                displayStatus.setTextColor(
                    root.context.getColor(
                        if (isOnline) R.color.success else R.color.error
                    )
                )
                
                // Checkbox state
                displayCheckbox.isChecked = selectedDisplayIds.contains(display.id)
                
                // Click listeners
                root.setOnClickListener {
                    displayCheckbox.isChecked = !displayCheckbox.isChecked
                }
                
                displayCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDisplayIds.add(display.id)
                    } else {
                        selectedDisplayIds.remove(display.id)
                    }
                    onSelectionChanged(selectedDisplayIds)
                }
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
