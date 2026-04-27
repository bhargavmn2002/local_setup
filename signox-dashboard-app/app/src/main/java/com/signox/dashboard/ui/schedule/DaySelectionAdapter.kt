package com.signox.dashboard.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.DayOfWeek
import com.signox.dashboard.databinding.ItemDayCheckboxBinding

class DaySelectionAdapter(
    private val onDaySelected: (DayOfWeek, Boolean) -> Unit
) : ListAdapter<DayOfWeek, DaySelectionAdapter.DayViewHolder>(DayDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayCheckboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DayViewHolder(
        private val binding: ItemDayCheckboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(day: DayOfWeek) {
            binding.dayCheckbox.text = day.displayName
            binding.dayCheckbox.isChecked = day.isSelected
            
            binding.dayCheckbox.setOnCheckedChangeListener { _, isChecked ->
                day.isSelected = isChecked
                onDaySelected(day, isChecked)
            }
        }
    }
    
    private class DayDiffCallback : DiffUtil.ItemCallback<DayOfWeek>() {
        override fun areItemsTheSame(oldItem: DayOfWeek, newItem: DayOfWeek): Boolean {
            return oldItem.name == newItem.name
        }
        
        override fun areContentsTheSame(oldItem: DayOfWeek, newItem: DayOfWeek): Boolean {
            return oldItem == newItem
        }
    }
}
