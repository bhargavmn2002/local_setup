package com.signox.dashboard.ui.display

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Playlist

class PlaylistSelectionAdapter(
    private val onSelectionChanged: (String?) -> Unit
) : ListAdapter<Playlist, PlaylistSelectionAdapter.ViewHolder>(PlaylistDiffCallback()) {
    
    private var selectedPlaylistId: String? = null
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_selection, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist, playlist.id == selectedPlaylistId)
    }
    
    fun getSelectedPlaylistId(): String? = selectedPlaylistId
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val radioButton: RadioButton = itemView.findViewById(R.id.radioButton)
        private val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        private val playlistInfo: TextView = itemView.findViewById(R.id.playlistInfo)
        
        fun bind(playlist: Playlist, isSelected: Boolean) {
            playlistName.text = playlist.name
            
            val itemCount = playlist.itemCount
            playlistInfo.text = "$itemCount item(s)"
            
            radioButton.isChecked = isSelected
            
            itemView.setOnClickListener {
                val previousSelected = selectedPlaylistId
                selectedPlaylistId = playlist.id
                
                // Notify the dialog about selection change
                onSelectionChanged(selectedPlaylistId)
                
                // Notify changes for both old and new selection
                if (previousSelected != null) {
                    val previousIndex = currentList.indexOfFirst { it.id == previousSelected }
                    if (previousIndex != -1) {
                        notifyItemChanged(previousIndex)
                    }
                }
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }
    
    private class PlaylistDiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}
