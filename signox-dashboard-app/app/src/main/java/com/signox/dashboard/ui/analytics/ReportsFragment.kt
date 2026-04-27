package com.signox.dashboard.ui.analytics

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.dashboard.databinding.FragmentReportsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ReportsFragment : Fragment() {
    
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsViewModel by viewModels()
    private lateinit var reportsAdapter: ReportsAdapter
    
    private var startDate: String? = null
    private var endDate: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupButtons()
        setupRefresh()
        
        // Load initial data
        viewModel.loadReports()
    }
    
    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter(
            onDeleteClick = { report ->
                showDeleteConfirmation(report.id)
            }
        )
        binding.rvReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reports.collect { reports ->
                if (reports.isNotEmpty()) {
                    binding.rvReports.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    reportsAdapter.submitList(reports)
                } else {
                    binding.rvReports.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
                binding.fabGenerateReport.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun setupButtons() {
        binding.fabGenerateReport.setOnClickListener {
            showGenerateReportDialog()
        }
    }
    
    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadReports()
        }
    }
    
    private fun showGenerateReportDialog() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Show date picker for start date
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                startDate = dateFormat.format(calendar.time)
                
                // Show date picker for end date
                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        calendar.set(endYear, endMonth, endDay)
                        endDate = dateFormat.format(calendar.time)
                        
                        // Generate report
                        generateReport()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun generateReport() {
        if (startDate != null && endDate != null) {
            val reportName = "Report ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}"
            viewModel.generateReport(
                name = reportName,
                type = "playback",
                startDate = startDate!!,
                endDate = endDate!!
            )
            Toast.makeText(requireContext(), "Generating report...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteConfirmation(reportId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteReport(reportId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
