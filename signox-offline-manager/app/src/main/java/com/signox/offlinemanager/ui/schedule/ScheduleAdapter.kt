package com.signox.offlinemanager.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.ContentType
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(
    private val onScheduleClick: (Schedule) -> Unit,
    private val onEditClick: (Schedule) -> Unit,
    private val onToggleStatus: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit,
    private val formatDaysOfWeek: (String) -> String,
    private val getContentName: (ContentType, Long) -> String
) : ListAdapter<Schedule, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
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
        private val binding: ItemScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(schedule: Schedule) {
            binding.apply {
                textScheduleName.text = schedule.name
                textScheduleDescription.text = schedule.description ?: "No description"
                textScheduleTime.text = "${schedule.startTime} - ${schedule.endTime}"
                textScheduleDays.text = formatDaysOfWeek(schedule.daysOfWeek)
                
                val contentName = getContentName(schedule.contentType, schedule.contentId)
                val contentTypeText = when (schedule.contentType) {
                    ContentType.PLAYLIST -> "Playlist"
                    ContentType.LAYOUT -> "Layout"
                }
                textScheduleContent.text = "$contentTypeText: $contentName"
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textScheduleDate.text = "Created ${dateFormat.format(Date(schedule.createdAt))}"
                
                // Status indicator
                switchScheduleActive.isChecked = schedule.isActive
                switchScheduleActive.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != schedule.isActive) {
                        onToggleStatus(schedule)
                    }
                }
                
                // Status colors
                val statusColor = if (schedule.isActive) {
                    itemView.context.getColor(R.color.success)
                } else {
                    itemView.context.getColor(R.color.text_secondary)
                }
                textScheduleName.setTextColor(statusColor)
                
                root.setOnClickListener { onScheduleClick(schedule) }
                
                buttonScheduleOptions.setOnClickListener { view ->
                    showPopupMenu(view, schedule)
                }
            }
        }
        
        private fun showPopupMenu(view: View, schedule: Schedule) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_schedule_options, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_schedule -> {
                        onEditClick(schedule)
                        true
                    }
                    R.id.action_toggle_schedule -> {
                        onToggleStatus(schedule)
                        true
                    }
                    R.id.action_delete_schedule -> {
                        onDeleteClick(schedule)
                        true
                    }
                    else -> false
                }
            }
            
            // Update toggle text based on current status
            val toggleItem = popup.menu.findItem(R.id.action_toggle_schedule)
            toggleItem.title = if (schedule.isActive) "Disable" else "Enable"
            
            popup.show()
        }
    }
    
    class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem == newItem
        }
    }
}