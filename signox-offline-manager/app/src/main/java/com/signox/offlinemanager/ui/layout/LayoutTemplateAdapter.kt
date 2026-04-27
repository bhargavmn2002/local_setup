package com.signox.offlinemanager.ui.layout

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.R
import com.signox.offlinemanager.databinding.ItemLayoutTemplateBinding

class LayoutTemplateAdapter(
    private val onTemplateClick: (LayoutTemplate) -> Unit
) : ListAdapter<LayoutTemplate, LayoutTemplateAdapter.TemplateViewHolder>(TemplateDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemLayoutTemplateBinding.inflate(
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
        private val binding: ItemLayoutTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(template: LayoutTemplate) {
            binding.apply {
                textTemplateName.text = template.name
                textTemplateDescription.text = template.description
                
                // Set section count
                textTemplateSections.text = "${template.sections.size} sections"
                
                // Set template type based on template ID
                textTemplateType.text = when {
                    template.id.contains("grid") -> "Grid Layout"
                    template.id.contains("split") -> "Split Layout"
                    template.id.contains("main") -> "Main Layout"
                    template.id.contains("scroll") -> "Scroll Layout"
                    else -> "Custom Layout"
                }
                
                // Draw template preview
                drawTemplatePreview(template)
                
                root.setOnClickListener { onTemplateClick(template) }
            }
        }
        
        private fun drawTemplatePreview(template: LayoutTemplate) {
            val canvas = Canvas()
            val previewView = binding.imageTemplatePreview
            
            // Create a bitmap to draw on (48dp icon size)
            val bitmap = android.graphics.Bitmap.createBitmap(
                48, 48, android.graphics.Bitmap.Config.ARGB_8888
            )
            canvas.setBitmap(bitmap)
            
            // Background
            val backgroundPaint = Paint().apply {
                color = ContextCompat.getColor(previewView.context, R.color.surface_variant)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 48f, 48f, backgroundPaint)
            
            // Draw sections
            val mediaPaint = Paint().apply {
                color = ContextCompat.getColor(previewView.context, R.color.primary)
                style = Paint.Style.FILL
                alpha = 120
            }
            
            val textPaint = Paint().apply {
                color = ContextCompat.getColor(previewView.context, R.color.accent)
                style = Paint.Style.FILL
                alpha = 120
            }
            
            val borderPaint = Paint().apply {
                color = ContextCompat.getColor(previewView.context, R.color.text_primary)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            
            template.sections.forEach { section ->
                val left = (section.x / 100f) * 48f
                val top = (section.y / 100f) * 48f
                val right = left + (section.width / 100f) * 48f
                val bottom = top + (section.height / 100f) * 48f
                
                val rect = RectF(left, top, right, bottom)
                
                // Fill based on section type
                val fillPaint = if (section.type == SectionType.TEXT) textPaint else mediaPaint
                canvas.drawRect(rect, fillPaint)
                
                // Border
                canvas.drawRect(rect, borderPaint)
            }
            
            previewView.setImageBitmap(bitmap)
        }
    }
    
    class TemplateDiffCallback : DiffUtil.ItemCallback<LayoutTemplate>() {
        override fun areItemsTheSame(oldItem: LayoutTemplate, newItem: LayoutTemplate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LayoutTemplate, newItem: LayoutTemplate): Boolean {
            return oldItem == newItem
        }
    }
}