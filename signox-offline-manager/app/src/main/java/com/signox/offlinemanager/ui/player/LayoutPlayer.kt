package com.signox.offlinemanager.ui.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.MediaType
import java.io.File

class LayoutPlayer(
    private val context: Context,
    private val containerView: ViewGroup,
    private val layout: Layout,
    private val zones: List<LayoutZoneWithMedia>,
    private val onPlaybackStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    
    private val zoneViews = mutableMapOf<Long, View>()
    private val zonePlayers = mutableMapOf<Long, ExoPlayer>()
    private val zonePlaylistPlayers = mutableMapOf<Long, PlaylistPlayer>()
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    
    fun startPlayback() {
        if (zones.isEmpty()) {
            onError("No zones to display")
            return
        }
        
        setupLayoutContainer()
        createZoneViews()
        startZonePlayback()
        
        isPlaying = true
        onPlaybackStateChanged(true)
    }
    
    private fun setupLayoutContainer() {
        containerView.removeAllViews()
        
        // Set layout background color
        try {
            val backgroundColor = android.graphics.Color.parseColor(layout.backgroundColor)
            containerView.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            containerView.setBackgroundColor(android.graphics.Color.BLACK)
        }
    }
    
    private fun createZoneViews() {
        zones.forEach { zoneWithMedia ->
            val zone = zoneWithMedia.zone
            
            // Calculate zone position and size relative to container
            val zoneView = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    zone.width,
                    zone.height
                ).apply {
                    leftMargin = zone.x
                    topMargin = zone.y
                }
            }
            
            containerView.addView(zoneView)
            zoneViews[zone.id] = zoneView
            
            // Setup content for this zone
            setupZoneContent(zone.id, zoneWithMedia, zoneView)
        }
    }
    
    private fun setupZoneContent(zoneId: Long, zoneWithMedia: LayoutZoneWithMedia, zoneView: FrameLayout) {
        when {
            zoneWithMedia.media != null -> {
                // Single media item
                setupSingleMediaZone(zoneId, zoneWithMedia.media, zoneView)
            }
            zoneWithMedia.playlist != null && zoneWithMedia.playlistMedia.isNotEmpty() -> {
                // Playlist in zone
                setupPlaylistZone(zoneId, zoneWithMedia, zoneView)
            }
            else -> {
                // Empty zone - show placeholder
                setupEmptyZone(zoneView)
            }
        }
    }
    
    private fun setupSingleMediaZone(zoneId: Long, media: com.signox.offlinemanager.data.model.Media, zoneView: FrameLayout) {
        val mediaFile = File(media.filePath)
        if (!mediaFile.exists()) {
            onError("Media file not found: ${media.name}")
            return
        }
        
        when (media.type) {
            MediaType.IMAGE -> {
                val imageView = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                
                Glide.with(context)
                    .load(mediaFile)
                    .into(imageView)
                
                zoneView.addView(imageView)
            }
            
            MediaType.VIDEO, MediaType.AUDIO -> {
                val playerView = PlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                }
                
                val exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(mediaFile))
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE // Loop the video
                    prepare()
                    play()
                }
                
                playerView.player = exoPlayer
                zoneView.addView(playerView)
                zonePlayers[zoneId] = exoPlayer
            }
        }
    }
    
    private fun setupPlaylistZone(zoneId: Long, zoneWithMedia: LayoutZoneWithMedia, zoneView: FrameLayout) {
        // Create a mini player view for this zone
        val playerView = PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            useController = false
        }
        
        // Create an image view for images in this zone
        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        
        zoneView.addView(playerView)
        zoneView.addView(imageView)
        
        // Create a playlist player for this zone
        val playlistPlayer = PlaylistPlayer(
            context = context,
            playerView = playerView,
            imageView = imageView,
            playlist = zoneWithMedia.playlist!!,
            mediaItems = zoneWithMedia.playlistMedia,
            onPlaybackStateChanged = { /* Handle zone-specific state */ },
            onPlaybackCompleted = { 
                // Restart the playlist for continuous playback
                handler.postDelayed({
                    if (isPlaying) {
                        setupPlaylistZone(zoneId, zoneWithMedia, zoneView)
                    }
                }, 1000)
            },
            onError = { error ->
                onError("Zone ${zoneWithMedia.zone.name}: $error")
            }
        )
        
        zonePlaylistPlayers[zoneId] = playlistPlayer
        playlistPlayer.startPlayback()
    }
    
    private fun setupEmptyZone(zoneView: FrameLayout) {
        // Add a subtle border to show empty zones
        zoneView.setBackgroundColor(0x22FFFFFF) // Semi-transparent white
    }
    
    private fun startZonePlayback() {
        // All zone setup is done in createZoneViews
        // This method can be used for any additional coordination
    }
    
    fun pause() {
        isPlaying = false
        
        // Pause all zone players
        zonePlayers.values.forEach { player ->
            player.pause()
        }
        
        zonePlaylistPlayers.values.forEach { player ->
            player.pause()
        }
        
        onPlaybackStateChanged(false)
    }
    
    fun resume() {
        isPlaying = true
        
        // Resume all zone players
        zonePlayers.values.forEach { player ->
            player.play()
        }
        
        zonePlaylistPlayers.values.forEach { player ->
            player.resume()
        }
        
        onPlaybackStateChanged(true)
    }
    
    fun stop() {
        isPlaying = false
        
        // Stop and release all players
        zonePlayers.values.forEach { player ->
            player.stop()
            player.release()
        }
        zonePlayers.clear()
        
        zonePlaylistPlayers.values.forEach { player ->
            player.stop()
        }
        zonePlaylistPlayers.clear()
        
        // Clear all views
        containerView.removeAllViews()
        zoneViews.clear()
        
        onPlaybackStateChanged(false)
    }
    
    fun isPlaying(): Boolean {
        return isPlaying
    }
}