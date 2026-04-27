package com.signox.offlinemanager.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.signox.offlinemanager.R
import com.signox.offlinemanager.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.cardMediaLibrary.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_media)
        }
        
        binding.cardPlaylists.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_playlists)
        }
        
        binding.cardLayouts.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_layouts)
        }
        
        binding.cardSchedules.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_schedules)
        }
        
        binding.cardPlayer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_player)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}