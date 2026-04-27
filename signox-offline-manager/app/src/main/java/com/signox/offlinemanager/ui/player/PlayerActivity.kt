package com.signox.offlinemanager.ui.player

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.signox.offlinemanager.databinding.ActivityPlayerBinding
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var viewModel: PlayerActivityViewModel
    private var playlistPlayer: PlaylistPlayer? = null
    private var layoutPlayer: LayoutPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable full-screen kiosk mode
        enableKioskMode()
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewModel()
        setupClickListeners()
        handleIntent()
    }
    
    private fun enableKioskMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide system UI for immersive kiosk experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        supportActionBar?.hide()
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val layoutRepository = LayoutRepository(database.layoutDao())
        val mediaRepository = MediaRepository(database.mediaDao())
        val scheduleRepository = ScheduleRepository(database.scheduleDao())
        
        val factory = PlayerActivityViewModelFactory(
            playlistRepository, layoutRepository, mediaRepository, scheduleRepository
        )
        viewModel = ViewModelProvider(this, factory)[PlayerActivityViewModel::class.java]
        
        // Disable player controls for kiosk mode
        binding.playerView.useController = false
    }
    
    private fun setupClickListeners() {
        // Exit button (hidden by default)
        binding.buttonExit.setOnClickListener {
            stopPlayback()
            finish()
        }
        
        // Tap anywhere to show/hide exit button
        binding.root.setOnClickListener {
            toggleExitButton()
        }
        
        // Hide status text after showing
        binding.textStatus.setOnClickListener {
            binding.textStatus.visibility = View.GONE
        }
    }
    
    private fun toggleExitButton() {
        if (binding.buttonExit.visibility == View.VISIBLE) {
            hideExitButton()
        } else {
            showExitButton()
        }
    }
    
    private fun showExitButton() {
        binding.buttonExit.visibility = View.VISIBLE
        binding.buttonExit.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        
        // Auto-hide after 3 seconds
        binding.buttonExit.postDelayed({
            hideExitButton()
        }, 3000)
    }
    
    private fun hideExitButton() {
        binding.buttonExit.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.buttonExit.visibility = View.GONE
            }
            .start()
    }
    
    private fun showStatus(message: String, autoHide: Boolean = true) {
        binding.textStatus.text = message
        binding.textStatus.visibility = View.VISIBLE
        binding.textStatus.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        
        if (autoHide) {
            binding.textStatus.postDelayed({
                binding.textStatus.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.textStatus.visibility = View.GONE
                    }
                    .start()
            }, 2000)
        }
    }
    
    private fun handleIntent() {
        val playScheduled = intent.getBooleanExtra("PLAY_SCHEDULED", false)
        
        if (playScheduled) {
            playScheduledContent()
        } else {
            val contentType = intent.getStringExtra("CONTENT_TYPE")
            val contentId = intent.getLongExtra("CONTENT_ID", -1)
            val contentName = intent.getStringExtra("CONTENT_NAME") ?: "Unknown"
            
            if (contentId != -1L && contentType != null) {
                playContent(contentType, contentId, contentName)
            } else {
                showStatus("No content selected", false)
            }
        }
    }
    
    private fun playScheduledContent() {
        lifecycleScope.launch {
            val scheduledContent = viewModel.getScheduledContent()
            if (scheduledContent != null) {
                showStatus("Playing scheduled: ${scheduledContent.name}")
                playContent(scheduledContent.type.name, scheduledContent.id, scheduledContent.name)
            } else {
                showStatus("No scheduled content available", false)
            }
        }
    }
    
    private fun playContent(contentType: String, contentId: Long, contentName: String) {
        showStatus("Loading $contentName...")
        
        when (contentType) {
            "PLAYLIST" -> {
                playPlaylist(contentId)
            }
            "LAYOUT" -> {
                playLayout(contentId)
            }
            else -> {
                showStatus("Unknown content type", false)
            }
        }
    }
    
    private fun playPlaylist(playlistId: Long) {
        lifecycleScope.launch {
            val playlistWithMedia = viewModel.getPlaylistWithMedia(playlistId)
            if (playlistWithMedia != null && playlistWithMedia.second.isNotEmpty()) {
                playlistPlayer = PlaylistPlayer(
                    context = this@PlayerActivity,
                    playerView = binding.playerView,
                    imageView = binding.imageView,
                    playlist = playlistWithMedia.first,
                    mediaItems = playlistWithMedia.second,
                    onPlaybackStateChanged = { isPlaying ->
                        // No UI updates needed for kiosk mode
                    },
                    onPlaybackCompleted = {
                        showStatus("Playback completed")
                    },
                    onError = { error ->
                        showStatus("Error: $error", false)
                    }
                )
                
                playlistPlayer?.startPlayback()
                showStatus("Playing ${playlistWithMedia.first.name}")
            } else {
                showStatus("Playlist is empty or not found", false)
            }
        }
    }
    
    private fun playLayout(layoutId: Long) {
        lifecycleScope.launch {
            val layoutWithZones = viewModel.getLayoutWithZones(layoutId)
            if (layoutWithZones != null && layoutWithZones.second.isNotEmpty()) {
                layoutPlayer = LayoutPlayer(
                    context = this@PlayerActivity,
                    containerView = binding.layoutContainer,
                    layout = layoutWithZones.first,
                    zones = layoutWithZones.second,
                    onPlaybackStateChanged = { isPlaying ->
                        // No UI updates needed for kiosk mode
                    },
                    onError = { error ->
                        showStatus("Error: $error", false)
                    }
                )
                
                layoutPlayer?.startPlayback()
                showStatus("Playing ${layoutWithZones.first.name}")
                binding.playerView.visibility = View.GONE
                binding.layoutContainer.visibility = View.VISIBLE
            } else {
                showStatus("Layout is empty or not found", false)
            }
        }
    }
    
    private fun stopPlayback() {
        playlistPlayer?.stop()
        layoutPlayer?.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
    
    override fun onBackPressed() {
        // Show exit button instead of immediately going back
        showExitButton()
    }
    
    // Keep screen on during playback
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}