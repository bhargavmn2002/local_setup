package com.signox.dashboard.ui.playlist

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.dashboard.BuildConfig
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.data.model.PlaylistItem
import com.signox.dashboard.data.model.PlaylistItemRequest
import com.signox.dashboard.databinding.ItemPlaylistItemBinding
import java.util.Collections

class PlaylistItemsAdapter(
    private val onRemoveClick: (PlaylistItem) -> Unit,
    private val onDurationChange: (PlaylistItem, Int) -> Unit
) : RecyclerView.Adapter<PlaylistItemsAdapter.ItemViewHolder>() {
    
    private val items = mutableListOf<PlaylistItem>()
    private var nextOrder = 0
    
    fun submitList(newItems: List<PlaylistItem>) {
        items.clear()
        items.addAll(newItems)
        nextOrder = items.size
        notifyDataSetChanged()
    }
    
    fun addMediaItems(mediaList: List<Media>) {
        mediaList.forEach { media ->
            val item = PlaylistItem(
                id = "",
                playlistId = "",
                mediaId = media.id,
                duration = media.duration ?: 10,
                order = nextOrder++,
                loopVideo = false,
                orientation = null,
                resizeMode = "FIT",
                rotation = 0,
                media = media
            )
            items.add(item)
        }
        notifyDataSetChanged()
    }
    
    fun removeItem(item: PlaylistItem) {
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
            reorderItems()
        }
    }
    
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        reorderItems()
    }
    
    fun updateItemDuration(item: PlaylistItem, duration: Int) {
        val position = items.indexOf(item)
        if (position != -1) {
            items[position] = item.copy(duration = duration)
        }
    }
    
    private fun reorderItems() {
        items.forEachIndexed { index, item ->
            items[index] = item.copy(order = index)
        }
    }
    
    fun getPlaylistItems(): List<PlaylistItemRequest> {
        return items.mapIndexed { index, item ->
            PlaylistItemRequest(
                mediaId = item.mediaId,
                duration = item.duration,
                order = index,
                loopVideo = item.loopVideo,
                orientation = item.orientation,
                resizeMode = item.resizeMode,
                rotation = item.rotation
            )
        }
    }
    
    fun getTotalDuration(): Int {
        return items.sumOf { it.effectiveDuration }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemPlaylistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class ItemViewHolder(
        private val binding: ItemPlaylistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentItem: PlaylistItem? = null
        
        init {
            binding.editTextDuration.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val duration = s.toString().toIntOrNull() ?: 10
                    currentItem?.let { item ->
                        onDurationChange(item, duration)
                    }
                }
            })
        }
        
        fun bind(item: PlaylistItem, position: Int) {
            currentItem = item
            
            binding.apply {
                textOrder.text = "${position + 1}"
                textMediaName.text = item.media.name
                textMediaType.text = item.media.type
                editTextDuration.setText(item.effectiveDuration.toString())
                
                // Load thumbnail
                val thumbnailUrl = item.media.thumbnailUrl ?: item.media.url
                val fullUrl = "${BuildConfig.API_BASE_URL.replace("/api/", "")}$thumbnailUrl"
                
                Glide.with(root.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_launcher)
                    .error(R.drawable.ic_launcher)
                    .centerCrop()
                    .into(imageThumbnail)
                
                buttonRemove.setOnClickListener {
                    onRemoveClick(item)
                }
            }
        }
    }
}
