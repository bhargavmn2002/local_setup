package com.signox.offlinemanager.ui.playlist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.databinding.ActivityPlaylistEditorBinding
import com.signox.offlinemanager.ui.media.MediaAdapter
import java.util.Collections

class PlaylistEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistEditorBinding
    private lateinit var viewModel: PlaylistEditorViewModel
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var playlistItemsAdapter: PlaylistItemsAdapter

    private var playlistId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playlistId = intent.getLongExtra("playlist_id", -1)
        if (playlistId == -1L) {
            Toast.makeText(this, "Invalid playlist ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewModel()
        setupRecyclerViews()
        setupClickListeners()
        setupSearch()
        observeViewModel()

        viewModel.loadPlaylist(playlistId)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val mediaRepository = MediaRepository(database.mediaDao())
        
        val factory = PlaylistEditorViewModelFactory(playlistRepository, mediaRepository)
        viewModel = ViewModelProvider(this, factory)[PlaylistEditorViewModel::class.java]
    }

    private fun setupRecyclerViews() {
        // Media library adapter
        mediaAdapter = MediaAdapter(
            onPreviewClick = { media ->
                viewModel.addMediaToPlaylist(media)
            },
            onDeleteClick = { /* Not used in this context */ }
        )

        binding.rvMediaLibrary.apply {
            layoutManager = GridLayoutManager(this@PlaylistEditorActivity, 2)
            adapter = mediaAdapter
        }

        // Playlist items adapter with drag and drop
        playlistItemsAdapter = PlaylistItemsAdapter(
            onRemoveClick = { playlistItem ->
                viewModel.removeMediaFromPlaylist(playlistItem)
            },
            onDurationChanged = { playlistItem, newDurationMs ->
                viewModel.updateItemDuration(playlistItem, newDurationMs)
            }
        )

        binding.rvPlaylistItems.apply {
            layoutManager = LinearLayoutManager(this@PlaylistEditorActivity)
            adapter = playlistItemsAdapter
        }

        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                viewModel.movePlaylistItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvPlaylistItems)
    }

    private fun setupClickListeners() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            viewModel.savePlaylist()
            Toast.makeText(this, "Playlist saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnPreview.setOnClickListener {
            // TODO: Implement playlist preview
            Toast.makeText(this, "Preview coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        binding.etMediaSearch.addTextChangedListener { text ->
            viewModel.searchMedia(text.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.playlist.observe(this) { playlist ->
            playlist?.let {
                binding.tvPlaylistName.text = it.name
                updatePlaylistInfo()
            }
        }

        viewModel.playlistItems.observe(this) { items ->
            playlistItemsAdapter.submitList(items)
            updatePlaylistInfo()
            updateEmptyState(items.isEmpty())
        }

        viewModel.filteredMedia.observe(this) { media ->
            mediaAdapter.submitList(media)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updatePlaylistInfo() {
        val items = viewModel.playlistItems.value ?: emptyList()
        val itemCount = items.size
        val totalDuration = items.sumOf { it.playlistItem.duration }
        
        val minutes = totalDuration / 60000
        val seconds = (totalDuration % 60000) / 1000
        val durationText = String.format("%d:%02d", minutes, seconds)
        
        binding.tvPlaylistInfo.text = "$itemCount items • $durationText duration"
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyPlaylist.visibility = if (isEmpty) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}