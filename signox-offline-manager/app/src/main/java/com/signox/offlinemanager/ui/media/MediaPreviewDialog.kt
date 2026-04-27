package com.signox.offlinemanager.ui.media

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.databinding.DialogMediaPreviewBinding
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class MediaPreviewDialog(
    context: Context,
    private val media: Media,
    private val onDeleteClick: (Media) -> Unit
) : Dialog(context) {
    
    private lateinit var binding: DialogMediaPreviewBinding
    private var exoPlayer: ExoPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        binding = DialogMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set dialog properties
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.8).toInt()
        )
        
        setupViews()
        setupPreview()
    }
    
    private fun setupViews() {
        binding.apply {
            // Set title
            tvPreviewTitle.text = media.name
            
            // Set details
            tvDetailFileSize.text = formatFileSize(media.fileSize)
            tvDetailCreated.text = formatDate(media.createdAt)
            
            // Show duration for videos and audio
            if (media.type == MediaType.VIDEO || media.type == MediaType.AUDIO) {
                layoutDurationDetail.visibility = View.VISIBLE
                tvDetailDuration.text = formatDuration(media.duration)
            } else {
                layoutDurationDetail.visibility = View.GONE
            }
            
            // Set click listeners
            btnClose.setOnClickListener { dismiss() }
            btnOk.setOnClickListener { dismiss() }
            btnDeleteFromPreview.setOnClickListener {
                onDeleteClick(media)
                dismiss()
            }
        }
    }
    
    private fun setupPreview() {
        val file = File(media.filePath)
        if (!file.exists()) {
            // File doesn't exist, show error
            binding.tvPreviewTitle.text = "${media.name} (File not found)"
            return
        }
        
        when (media.type) {
            MediaType.IMAGE -> {
                binding.ivPreviewImage.visibility = View.VISIBLE
                binding.pvPreviewVideo.visibility = View.GONE
                binding.layoutAudioPreview.visibility = View.GONE
                
                Glide.with(context)
                    .load(file)
                    .fitCenter()
                    .into(binding.ivPreviewImage)
            }
            
            MediaType.VIDEO -> {
                binding.ivPreviewImage.visibility = View.GONE
                binding.pvPreviewVideo.visibility = View.VISIBLE
                binding.layoutAudioPreview.visibility = View.GONE
                
                setupVideoPlayer(file)
            }
            
            MediaType.AUDIO -> {
                binding.ivPreviewImage.visibility = View.GONE
                binding.pvPreviewVideo.visibility = View.GONE
                binding.layoutAudioPreview.visibility = View.VISIBLE
                
                // For audio, we could set up an audio player here
                // For now, just show the placeholder
            }
        }
    }
    
    private fun setupVideoPlayer(file: File) {
        try {
            exoPlayer = ExoPlayer.Builder(context).build()
            binding.pvPreviewVideo.player = exoPlayer
            
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            // If video player fails, hide it and show error
            binding.pvPreviewVideo.visibility = View.GONE
            binding.tvPreviewTitle.text = "${media.name} (Preview unavailable)"
        }
    }
    
    override fun dismiss() {
        releasePlayer()
        super.dismiss()
    }
    
    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
    
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        
        return DecimalFormat("#,##0.#").format(
            bytes / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format("%d:%02d", minutes, seconds % 60)
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}