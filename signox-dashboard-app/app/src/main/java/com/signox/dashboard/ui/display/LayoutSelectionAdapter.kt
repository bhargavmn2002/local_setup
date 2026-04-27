package com.signox.dashboard.ui.display

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Layout
import com.signox.dashboard.databinding.ItemLayoutSelectionBinding

class LayoutSelectionAdapter(
    private val onSelectionChanged: (String?) -> Unit
) : ListAdapter<Layout, LayoutSelectionAdapter.ViewHolder>(LayoutDiffCallback()) {

    private var selectedLayoutId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLayoutSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedLayoutId(): String? = selectedLayoutId

    inner class ViewHolder(
        private val binding: ItemLayoutSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(layout: Layout) {
            binding.apply {
                tvLayoutName.text = layout.name
                tvLayoutDescription.text = layout.description ?: "No description"
                tvLayoutDimensions.text = "${layout.width}x${layout.height}"
                tvSectionCount.text = "${layout.sections?.size ?: 0} sections"

                // Status indicator
                val statusColor = if (layout.isActive) {
                    R.color.status_online
                } else {
                    R.color.status_offline
                }
                viewStatusIndicator.setBackgroundResource(statusColor)

                // Selection state
                radioButton.isChecked = layout.id == selectedLayoutId

                // Click listener
                root.setOnClickListener {
                    val previousSelection = selectedLayoutId
                    selectedLayoutId = if (selectedLayoutId == layout.id) {
                        null // Deselect if clicking the same item
                    } else {
                        layout.id
                    }
                    
                    // Notify adapter to update UI
                    if (previousSelection != null) {
                        notifyItemChanged(currentList.indexOfFirst { it.id == previousSelection })
                    }
                    notifyItemChanged(bindingAdapterPosition)
                    
                    onSelectionChanged(selectedLayoutId)
                }

                radioButton.setOnClickListener {
                    root.performClick()
                }
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
