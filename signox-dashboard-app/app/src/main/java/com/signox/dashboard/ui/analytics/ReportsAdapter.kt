package com.signox.dashboard.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.Report
import com.signox.dashboard.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.*

class ReportsAdapter(
    private val onDeleteClick: (Report) -> Unit
) : ListAdapter<Report, ReportsAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemReportBinding,
        private val onDeleteClick: (Report) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(report: Report) {
            binding.apply {
                tvReportName.text = report.name
                tvReportType.text = report.type.uppercase()
                
                // Date range
                try {
                    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(report.dateRange.startDate)
                    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(report.dateRange.endDate)
                    
                    val dateRangeText = "${startDate?.let { dateFormat.format(it) }} - ${endDate?.let { dateFormat.format(it) }}"
                    tvDateRange.text = dateRangeText
                } catch (e: Exception) {
                    tvDateRange.text = "${report.dateRange.startDate} - ${report.dateRange.endDate}"
                }
                
                // Status
                tvStatus.text = report.status.uppercase()
                tvStatus.setTextColor(
                    when (report.status) {
                        "completed" -> android.graphics.Color.parseColor("#4CAF50")
                        "pending" -> android.graphics.Color.parseColor("#FF9800")
                        "failed" -> android.graphics.Color.parseColor("#F44336")
                        else -> android.graphics.Color.parseColor("#757575")
                    }
                )
                
                // Summary
                report.summary?.let { summary ->
                    tvSummary.text = "${summary.totalPlaybacks} playbacks • ${summary.displaysIncluded} displays"
                    tvSummary.visibility = android.view.View.VISIBLE
                } ?: run {
                    tvSummary.visibility = android.view.View.GONE
                }
                
                // Delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(report)
                }
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}
