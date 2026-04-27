package com.signox.offlinemanager.ui.playlist

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.databinding.DialogCreatePlaylistBinding

class CreatePlaylistDialog(
    private val context: Context,
    private val playlist: Playlist? = null,
    private val onPlaylistSaved: (String, String?) -> Unit
) {

    fun show() {
        val binding = DialogCreatePlaylistBinding.inflate(LayoutInflater.from(context))
        
        // Set title based on whether we're creating or editing
        binding.tvDialogTitle.text = if (playlist == null) {
            "Create New Playlist"
        } else {
            "Edit Playlist"
        }

        binding.btnSave.text = if (playlist == null) {
            "Create"
        } else {
            "Save"
        }

        // Pre-fill fields if editing
        playlist?.let {
            binding.etPlaylistName.setText(it.name)
            binding.etPlaylistDescription.setText(it.description)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        // Setup click listeners
        binding.btnCancel.setOnClickListener {
            android.util.Log.d("CreatePlaylistDialog", "Cancel button clicked")
            dialog.dismiss()
        }

        binding.btnSave.setOnClickListener {
            android.util.Log.d("CreatePlaylistDialog", "Save button clicked")
            val name = binding.etPlaylistName.text.toString().trim()
            val description = binding.etPlaylistDescription.text.toString().trim()

            if (name.isBlank()) {
                binding.etPlaylistName.error = "Playlist name is required"
                return@setOnClickListener
            }

            android.util.Log.d("CreatePlaylistDialog", "Attempting to save playlist: name='$name', description='$description'")
            
            onPlaylistSaved(name, description.ifBlank { null })
            dialog.dismiss()
        }

        dialog.show()
        
        // Focus on name field
        binding.etPlaylistName.requestFocus()
    }
}