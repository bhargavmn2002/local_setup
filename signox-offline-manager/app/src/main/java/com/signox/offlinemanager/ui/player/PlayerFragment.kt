package com.signox.offlinemanager.ui.player

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.offlinemanager.databinding.FragmentPlayerBinding
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository

class PlayerFragment : Fragment() {
    
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PlayerViewModel
    private lateinit var contentAdapter: PlayerContentAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val layoutRepository = LayoutRepository(database.layoutDao())
        val scheduleRepository = ScheduleRepository(database.scheduleDao())
        
        val factory = PlayerViewModelFactory(playlistRepository, layoutRepository, scheduleRepository)
        viewModel = ViewModelProvider(this, factory)[PlayerViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        contentAdapter = PlayerContentAdapter { contentItem ->
            startFullScreenPlayer(contentItem)
        }
        
        binding.recyclerViewContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonPlayScheduled.setOnClickListener {
            startScheduledPlayer()
        }
        
        binding.buttonRefresh.setOnClickListener {
            viewModel.refreshContent()
        }
    }
    
    private fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { items ->
            contentAdapter.submitList(items)
            binding.textEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.currentSchedule.observe(viewLifecycleOwner) { schedule ->
            binding.textCurrentSchedule.text = if (schedule != null) {
                "Current: ${schedule.name} (${schedule.startTime} - ${schedule.endTime})"
            } else {
                "No active schedule"
            }
        }
    }
    
    private fun startFullScreenPlayer(contentItem: PlayerContentItem) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("CONTENT_TYPE", contentItem.type.name)
            putExtra("CONTENT_ID", contentItem.id)
            putExtra("CONTENT_NAME", contentItem.name)
        }
        startActivity(intent)
    }
    
    private fun startScheduledPlayer() {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("PLAY_SCHEDULED", true)
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}