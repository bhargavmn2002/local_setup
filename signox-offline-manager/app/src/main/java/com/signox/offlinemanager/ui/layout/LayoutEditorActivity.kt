package com.signox.offlinemanager.ui.layout

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.offlinemanager.R
import com.signox.offlinemanager.SignoXOfflineApplication
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.databinding.ActivityLayoutEditorBinding

class LayoutEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLayoutEditorBinding
    private val viewModel: LayoutEditorViewModel by viewModels {
        val app = application as SignoXOfflineApplication
        val layoutRepository = LayoutRepository(app.database.layoutDao())
        val mediaRepository = MediaRepository(app.database.mediaDao())
        LayoutEditorViewModelFactory(layoutRepository, mediaRepository)
    }
    
    private lateinit var mediaAdapter: LayoutMediaAdapter
    private lateinit var zoneAdapter: LayoutZoneAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLayoutEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerViews()
        setupCanvasView()
        observeViewModel()
        
        val layoutId = intent.getLongExtra("layout_id", -1)
        if (layoutId != -1L) {
            viewModel.loadLayout(layoutId)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Layout Editor"
    }
    
    private fun setupRecyclerViews() {
        // Media library adapter
        mediaAdapter = LayoutMediaAdapter { media ->
            viewModel.selectMedia(media)
        }
        
        binding.recyclerViewMedia.apply {
            adapter = mediaAdapter
            layoutManager = LinearLayoutManager(this@LayoutEditorActivity)
        }
        
        // Zone list adapter
        zoneAdapter = LayoutZoneAdapter(
            onZoneClick = { zone -> viewModel.selectZone(zone) },
            onDeleteZone = { zone -> viewModel.deleteZone(zone) }
        )
        
        binding.recyclerViewZones.apply {
            adapter = zoneAdapter
            layoutManager = LinearLayoutManager(this@LayoutEditorActivity)
        }
    }
    
    private fun setupCanvasView() {
        binding.layoutCanvas.setOnZoneSelectedListener { zone ->
            viewModel.selectZone(zone)
        }
        
        // Remove zone creation functionality - zones come from templates only
    }
    
    private fun observeViewModel() {
        viewModel.layout.observe(this) { layout ->
            layout?.let {
                supportActionBar?.title = "Edit: ${it.name}"
                binding.layoutCanvas.setLayoutDimensions(it.width, it.height)
            }
        }
        
        viewModel.zones.observe(this) { zones ->
            zoneAdapter.submitList(zones)
            binding.layoutCanvas.setZones(zones)
        }
        
        viewModel.mediaList.observe(this) { mediaList ->
            mediaAdapter.submitList(mediaList)
        }
        
        viewModel.selectedZone.observe(this) { zone ->
            binding.layoutCanvas.selectZone(zone)
            updateZoneProperties(zone)
        }
        
        viewModel.selectedMedia.observe(this) { media ->
            // Assign media to selected zone
            viewModel.selectedZone.value?.let { zone ->
                viewModel.assignMediaToZone(zone, media)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun updateZoneProperties(zone: com.signox.offlinemanager.data.model.LayoutZone?) {
        // Update zone properties panel
        zone?.let {
            binding.textZoneName.text = it.name
            binding.textZonePosition.text = "${it.x}, ${it.y}"
            binding.textZoneSize.text = "${it.width} × ${it.height}"
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_layout_editor, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save_layout -> {
                viewModel.saveLayout()
                true
            }
            R.id.action_preview_layout -> {
                // TODO: Implement preview
                Toast.makeText(this, "Preview coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}