package com.signox.dashboard.ui.playlist

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.signox.dashboard.databinding.FragmentPlaylistPreviewBinding
import com.signox.dashboard.data.model.Playlist
import com.signox.dashboard.data.model.PlaylistItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistPreviewFragment : Fragment() {
    
    private var _binding: FragmentPlaylistPreviewBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlaylistViewModel by viewModels()
    
    private var playlist: Playlist? = null
    private var currentItemIndex = 0
    private var isPlaying = false
    private var isLooping = true
    
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var itemStartTime = 0L
    private var currentItemDuration = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val playlistId = arguments?.getString("playlistId")
        
        setupControls()
        observeViewModel()
        
        // Load playlist
        playlistId?.let { viewModel.loadPlaylist(it) }
    }
    
    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pausePlayback()
            } else {
                startPlayback()
            }
        }
        
        binding.btnStop.setOnClickListener {
            stopPlayback()
        }
        
        binding.btnPrevious.setOnClickListener {
            playPreviousItem()
        }
        
        binding.btnNext.setOnClickListener {
            playNextItem()
        }
        
        binding.switchLoop.setOnCheckedChangeListener { _, isChecked ->
            isLooping = isChecked
        }
        
        binding.btnClose.setOnClickListener {
            stopPlayback()
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentPlaylist.observe(viewLifecycleOwner) { playlist ->
            this.playlist = playlist
            if (playlist != null && !playlist.items.isNullOrEmpty()) {
                currentItemIndex = 0
                displayItem(0)
                updateControls()
            }
        }
    }
    
    private fun startPlayback() {
        val items = playlist?.items
        if (playlist == null || items.isNullOrEmpty()) return
        
        isPlaying = true
        binding.btnPlayPause.text = "Pause"
        itemStartTime = System.currentTimeMillis()
        
        displayItem(currentItemIndex)
        startProgressUpdates()
    }
    
    private fun pausePlayback() {
        isPlaying = false
        binding.btnPlayPause.text = "Play"
        stopProgressUpdates()
    }
    
    private fun stopPlayback() {
        isPlaying = false
        currentItemIndex = 0
        binding.btnPlayPause.text = "Play"
        stopProgressUpdates()
        displayItem(0)
    }
    
    private fun playNextItem() {
        val items = playlist?.items ?: return
        
        if (currentItemIndex < items.size - 1) {
            currentItemIndex++
            displayItem(currentItemIndex)
            if (isPlaying) {
                itemStartTime = System.currentTimeMillis()
                startProgressUpdates()
            }
        } else if (isLooping) {
            currentItemIndex = 0
            displayItem(currentItemIndex)
            if (isPlaying) {
                itemStartTime = System.currentTimeMillis()
                startProgressUpdates()
            }
        } else {
            stopPlayback()
        }
    }
    
    private fun playPreviousItem() {
        val items = playlist?.items ?: return
        
        if (currentItemIndex > 0) {
            currentItemIndex--
        } else if (isLooping) {
            currentItemIndex = items.size - 1
        }
        
        displayItem(currentItemIndex)
        if (isPlaying) {
            itemStartTime = System.currentTimeMillis()
            startProgressUpdates()
        }
    }
    
    private fun displayItem(index: Int) {
        val items = playlist?.items ?: return
        if (index < 0 || index >= items.size) return
        
        val item = items[index]
        currentItemDuration = item.duration ?: 10 // Default 10 seconds
        
        // Update info
        binding.tvItemName.text = item.media.name
        binding.tvItemInfo.text = "Item ${index + 1} of ${items.size} • ${currentItemDuration}s duration"
        
        // Display media
        val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/")
        val fullUrl = baseUrl + item.media.url
        
        when {
            item.media.isImage -> {
                binding.imagePreview.visibility = View.VISIBLE
                binding.videoPreview.visibility = View.GONE
                
                Glide.with(this)
                    .load(fullUrl)
                    .into(binding.imagePreview)
            }
            item.media.isVideo -> {
                binding.imagePreview.visibility = View.GONE
                binding.videoPreview.visibility = View.VISIBLE
                
                binding.videoPreview.setVideoURI(Uri.parse(fullUrl))
                if (isPlaying) {
                    binding.videoPreview.start()
                }
            }
        }
        
        updateControls()
    }
    
    private fun startProgressUpdates() {
        stopProgressUpdates()
        
        progressRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return
                
                val elapsed = (System.currentTimeMillis() - itemStartTime) / 1000
                val progress = ((elapsed.toFloat() / currentItemDuration) * 100).toInt()
                
                binding.progressBar.progress = progress.coerceIn(0, 100)
                binding.tvProgress.text = "${elapsed}s / ${currentItemDuration}s"
                
                if (elapsed >= currentItemDuration) {
                    // Move to next item
                    playNextItem()
                } else {
                    handler.postDelayed(this, 100) // Update every 100ms
                }
            }
        }
        
        handler.post(progressRunnable!!)
    }
    
    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }
    
    private fun updateControls() {
        val items = playlist?.items ?: return
        
        binding.btnPrevious.isEnabled = currentItemIndex > 0 || isLooping
        binding.btnNext.isEnabled = currentItemIndex < items.size - 1 || isLooping
    }
    
    override fun onPause() {
        super.onPause()
        pausePlayback()
        binding.videoPreview.pause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopProgressUpdates()
        _binding = null
    }
}
