package com.signox.dashboard.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Schedule
import com.signox.dashboard.databinding.ItemScheduleCardBinding

class ScheduleAdapter(
    private val onScheduleClick: (Schedule) -> Unit,
    private val onToggleActive: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ScheduleViewHolder(
        private val binding: ItemScheduleCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(schedule: Schedule) {
            binding.apply {
                scheduleName.text = schedule.name
                
                // Time range
                scheduleTime.text = "${schedule.startTime} - ${schedule.endTime}"
                
                // Content info
                val contentInfo = when {
                    schedule.playlist != null -> "Playlist: ${schedule.playlist.name}"
                    schedule.layout != null -> "Layout: ${schedule.layout.name}"
                    else -> "No content assigned"
                }
                scheduleContent.text = contentInfo
                
                // Days
                val daysText = schedule.repeatDays.joinToString(", ") { day ->
                    day.take(3).capitalize()
                }
                scheduleDays.text = daysText
                
                // Display count
                val displayCount = schedule.displays?.size ?: 0
                scheduleDisplays.text = "$displayCount display(s)"
                
                // Priority
                schedulePriority.text = "Priority: ${schedule.priority}"
                
                // Active status
                val statusColor = if (schedule.isActive) {
                    ContextCompat.getColor(root.context, R.color.success)
                } else {
                    ContextCompat.getColor(root.context, R.color.text_secondary)
                }
                scheduleStatus.setTextColor(statusColor)
                scheduleStatus.text = if (schedule.isActive) "Active" else "Inactive"
                
                // Click listeners
                root.setOnClickListener { onScheduleClick(schedule) }
                
                toggleButton.isChecked = schedule.isActive
                toggleButton.setOnClickListener {
                    onToggleActive(schedule)
                }
                
                deleteButton.setOnClickListener {
                    onDeleteClick(schedule)
                }
            }
        }
    }
    
    private class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem == newItem
        }
    }
}
