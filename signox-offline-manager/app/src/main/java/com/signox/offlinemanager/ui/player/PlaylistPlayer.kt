package com.signox.offlinemanager.ui.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.data.model.Playlist
import java.io.File

class PlaylistPlayer(
    private val context: Context,
    private val playerView: PlayerView,
    private val imageView: ImageView,
    private val playlist: Playlist,
    private val mediaItems: List<Media>,
    private val onPlaybackStateChanged: (Boolean) -> Unit,
    private val onPlaybackCompleted: () -> Unit,
    private val onError: (String) -> Unit
) {
    
    private var exoPlayer: ExoPlayer? = null
    private var currentMediaIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mediaTransitionRunnable: Runnable? = null
    private var isCurrentlyPlaying = false
    
    fun startPlayback() {
        if (mediaItems.isEmpty()) {
            onError("No media items to play")
            return
        }
        
        initializePlayer()
        playCurrentMedia()
    }
    
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isCurrentlyPlaying = isPlaying
                            onPlaybackStateChanged(isPlaying)
                        }
                        Player.STATE_ENDED -> {
                            handleMediaEnded()
                        }
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    onError("Playback error: ${error.message}")
                }
            })
        }
        
        playerView.player = exoPlayer
    }
    
    private fun playCurrentMedia() {
        if (currentMediaIndex >= mediaItems.size) {
            // Loop back to beginning for kiosk mode
            currentMediaIndex = 0
        }
        
        val media = mediaItems[currentMediaIndex]
        val mediaFile = File(media.filePath)
        
        if (!mediaFile.exists()) {
            onError("Media file not found: ${media.name}")
            moveToNextMedia()
            return
        }
        
        when (media.type) {
            MediaType.VIDEO, MediaType.AUDIO -> {
                playVideoOrAudio(media)
            }
            MediaType.IMAGE -> {
                playImage(media)
            }
        }
    }
    
    private fun playVideoOrAudio(media: Media) {
        // Hide image view, show player view
        imageView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(media.filePath)))
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        
        isCurrentlyPlaying = true
        onPlaybackStateChanged(true)
        
        // If custom duration is set and shorter than actual duration, schedule transition
        if (media.duration > 0) {
            scheduleMediaTransition(media.duration)
        }
    }
    
    private fun playImage(media: Media) {
        // Hide player view, show image view
        playerView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        
        // Stop any current video/audio playback
        exoPlayer?.stop()
        
        // Load image using Glide
        Glide.with(context)
            .load(File(media.filePath))
            .centerCrop()
            .into(imageView)
        
        val duration = if (media.duration > 0) media.duration else 5000L // Default 5 seconds
        
        isCurrentlyPlaying = true
        onPlaybackStateChanged(true)
        scheduleMediaTransition(duration)
    }
    
    private fun scheduleMediaTransition(duration: Long) {
        mediaTransitionRunnable?.let { handler.removeCallbacks(it) }
        
        mediaTransitionRunnable = Runnable {
            moveToNextMedia()
        }
        
        handler.postDelayed(mediaTransitionRunnable!!, duration)
    }
    
    private fun handleMediaEnded() {
        // This is called when video/audio naturally ends
        moveToNextMedia()
    }
    
    private fun moveToNextMedia() {
        currentMediaIndex++
        if (currentMediaIndex >= mediaItems.size) {
            // For kiosk mode, loop back to beginning instead of completing
            currentMediaIndex = 0
        }
        playCurrentMedia()
    }
    
    fun pause() {
        exoPlayer?.pause()
        mediaTransitionRunnable?.let { handler.removeCallbacks(it) }
        isCurrentlyPlaying = false
        onPlaybackStateChanged(false)
    }
    
    fun resume() {
        val currentMedia = mediaItems.getOrNull(currentMediaIndex)
        
        if (currentMedia?.type == MediaType.IMAGE) {
            // For images, restart the timer
            val duration = if (currentMedia.duration > 0) currentMedia.duration else 5000L
            scheduleMediaTransition(duration)
        } else {
            // For video/audio, resume ExoPlayer
            exoPlayer?.play()
        }
        
        isCurrentlyPlaying = true
        onPlaybackStateChanged(true)
    }
    
    fun stop() {
        mediaTransitionRunnable?.let { handler.removeCallbacks(it) }
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        
        // Hide both views
        playerView.visibility = View.GONE
        imageView.visibility = View.GONE
        
        isCurrentlyPlaying = false
        onPlaybackStateChanged(false)
    }
    
    fun isPlaying(): Boolean {
        return isCurrentlyPlaying
    }
}