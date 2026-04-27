package com.signox.offlinemanager.ui.layout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.databinding.ItemLayoutBinding
import java.text.SimpleDateFormat
import java.util.*

class LayoutAdapter(
    private val onLayoutClick: (Layout) -> Unit,
    private val onEditClick: (Layout) -> Unit,
    private val onPreviewClick: (Layout) -> Unit,
    private val onDeleteClick: (Layout) -> Unit
) : ListAdapter<Layout, LayoutAdapter.LayoutViewHolder>(LayoutDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutViewHolder {
        val binding = ItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LayoutViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class LayoutViewHolder(
        private val binding: ItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(layout: Layout) {
            binding.apply {
                textLayoutName.text = layout.name
                textLayoutDescription.text = layout.description ?: "No description"
                textLayoutDimensions.text = "${layout.width} × ${layout.height}"
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textLayoutDate.text = "Created ${dateFormat.format(Date(layout.createdAt))}"
                
                root.setOnClickListener { onLayoutClick(layout) }
                
                buttonLayoutOptions.setOnClickListener { view ->
                    showPopupMenu(view, layout)
                }
            }
        }
        
        private fun showPopupMenu(view: View, layout: Layout) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_layout_options, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_layout -> {
                        onEditClick(layout)
                        true
                    }
                    R.id.action_preview_layout -> {
                        onPreviewClick(layout)
                        true
                    }
                    R.id.action_delete_layout -> {
                        onDeleteClick(layout)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
    }
    
    class LayoutDiffCallback : DiffUtil.ItemCallback<Layout>() {
        override fun areItemsTheSame(oldItem: Layout, newItem: Layout): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Layout, newItem: Layout): Boolean {
            return oldItem == newItem
        }
    }
}