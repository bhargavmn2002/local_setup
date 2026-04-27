package com.signox.dashboard.ui.layout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.data.model.LayoutTemplate
import com.signox.dashboard.databinding.ItemTemplateCardBinding

class TemplateAdapter(
    private val onTemplateClick: (LayoutTemplate) -> Unit
) : ListAdapter<LayoutTemplate, TemplateAdapter.TemplateViewHolder>(TemplateDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TemplateViewHolder(
        private val binding: ItemTemplateCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(template: LayoutTemplate) {
            binding.apply {
                templateName.text = template.name
                templateDescription.text = template.description
                templateInfo.text = "${template.sections.size} sections • ${template.orientation}"
                templateThumbnail.setImageResource(template.thumbnail)
                
                root.setOnClickListener { onTemplateClick(template) }
            }
        }
    }
    
    private class TemplateDiffCallback : DiffUtil.ItemCallback<LayoutTemplate>() {
        override fun areItemsTheSame(oldItem: LayoutTemplate, newItem: LayoutTemplate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LayoutTemplate, newItem: LayoutTemplate): Boolean {
            return oldItem == newItem
        }
    }
}
