package com.signox.dashboard.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signox.dashboard.BuildConfig
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Playlist
import com.signox.dashboard.databinding.ItemPlaylistCardBinding

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onDeleteClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {
    
    private var playlists = listOf<Playlist>()
    private var filteredPlaylists = listOf<Playlist>()
    
    fun submitList(newPlaylists: List<Playlist>) {
        playlists = newPlaylists
        filteredPlaylists = newPlaylists
        notifyDataSetChanged()
    }
    
    fun filter(query: String) {
        filteredPlaylists = if (query.isEmpty()) {
            playlists
        } else {
            playlists.filter { playlist ->
                playlist.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(filteredPlaylists[position])
    }
    
    override fun getItemCount(): Int = filteredPlaylists.size
    
    inner class PlaylistViewHolder(
        private val binding: ItemPlaylistCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(playlist: Playlist) {
            binding.apply {
                textPlaylistName.text = playlist.name
                textItemCount.text = "${playlist.itemCount} items"
                textDuration.text = playlist.formattedDuration
                
                // Load thumbnail if available
                val thumbnailUrl = playlist.firstMediaUrl
                if (thumbnailUrl != null) {
                    val fullUrl = "${BuildConfig.API_BASE_URL.replace("/api/", "")}$thumbnailUrl"
                    Glide.with(root.context)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_launcher)
                        .error(R.drawable.ic_launcher)
                        .centerCrop()
                        .into(imagePlaylistThumbnail)
                } else {
                    imagePlaylistThumbnail.setImageResource(R.drawable.ic_launcher)
                }
                
                // Click listeners
                root.setOnClickListener {
                    onPlaylistClick(playlist)
                }
                
                buttonDelete.setOnClickListener {
                    onDeleteClick(playlist)
                }
            }
        }
    }
}
