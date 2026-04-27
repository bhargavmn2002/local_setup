package com.signox.dashboard.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.data.model.*
import com.signox.dashboard.databinding.FragmentScheduleEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ScheduleEditorFragment : Fragment() {
    
    private var _binding: FragmentScheduleEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ScheduleViewModel by viewModels()
    
    private val scheduleId: String? by lazy {
        arguments?.getString(ARG_SCHEDULE_ID)
    }
    
    private var selectedDays = mutableListOf<String>()
    private var selectedDisplayIds = mutableListOf<String>()
    private var selectedContentType: String? = null // "playlist" or "layout"
    private var selectedContentId: String? = null
    private var selectedContentName: String? = null
    
    private var startTime: String = "09:00"
    private var endTime: String = "17:00"
    private var startDate: String? = null
    private var endDate: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDaySelection()
        setupTimePickers()
        setupDatePickers()
        setupContentSelection()
        setupDisplaySelection()
        setupButtons()
        observeViewModel()
        
        // Load schedule if editing
        scheduleId?.let { id ->
            viewModel.loadSchedule(id)
        }
    }
    
    private fun loadScheduleData(schedule: Schedule) {
        // Load basic info
        binding.nameEditText.setText(schedule.name)
        binding.descriptionEditText.setText(schedule.description ?: "")
        binding.priorityEditText.setText(schedule.priority.toString())
        
        // Load times
        startTime = schedule.startTime
        endTime = schedule.endTime
        binding.startTimeButton.text = formatTime12Hour(startTime)
        binding.endTimeButton.text = formatTime12Hour(endTime)
        
        // Load dates
        startDate = schedule.startDate
        endDate = schedule.endDate
        binding.startDateButton.text = startDate ?: "Select Start Date"
        binding.endDateButton.text = endDate ?: "Select End Date"
        
        // Load selected days
        selectedDays.clear()
        selectedDays.addAll(schedule.repeatDays)
        updateDayCheckboxes()
        
        // Load content
        if (schedule.playlist != null) {
            selectedContentType = "playlist"
            selectedContentId = schedule.playlist.id
            selectedContentName = schedule.playlist.name
        } else if (schedule.layout != null) {
            selectedContentType = "layout"
            selectedContentId = schedule.layout.id
            selectedContentName = schedule.layout.name
        }
        updateContentDisplay()
        
        // Load displays
        selectedDisplayIds.clear()
        selectedDisplayIds.addAll(schedule.displays?.map { it.display?.id ?: "" }?.filter { it.isNotEmpty() } ?: emptyList())
        updateDisplayCount()
    }
    
    private fun updateDayCheckboxes() {
        for (i in 0 until binding.daysContainer.childCount) {
            val checkBox = binding.daysContainer.getChildAt(i) as? android.widget.CheckBox
            checkBox?.let {
                val dayName = DayOfWeek.getAll()[i].name
                it.isChecked = selectedDays.contains(dayName)
            }
        }
    }
    
    private fun setupDaySelection() {
        val allDays = DayOfWeek.getAll()
        
        // Dynamically add checkboxes for each day
        allDays.forEach { day ->
            val checkBox = android.widget.CheckBox(requireContext()).apply {
                text = day.displayName
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setPadding(8, 12, 8, 12)
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDays.add(day.name)
                    } else {
                        selectedDays.remove(day.name)
                    }
                }
            }
            
            binding.daysContainer.addView(checkBox)
        }
    }
    
    private fun setupTimePickers() {
        binding.startTimeButton.text = formatTime12Hour(startTime)
        binding.endTimeButton.text = formatTime12Hour(endTime)
        
        binding.startTimeButton.setOnClickListener {
            showTimePicker(startTime) { time ->
                startTime = time
                binding.startTimeButton.text = formatTime12Hour(time)
            }
        }
        
        binding.endTimeButton.setOnClickListener {
            showTimePicker(endTime) { time ->
                endTime = time
                binding.endTimeButton.text = formatTime12Hour(time)
            }
        }
    }
    
    private fun formatTime12Hour(time24: String): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            val period = if (hour >= 12) "PM" else "AM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            
            String.format("%d:%02d %s", hour12, minute, period)
        } catch (e: Exception) {
            time24
        }
    }
    
    private fun parse12HourTo24Hour(time12: String): String {
        return try {
            val parts = time12.split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val period = parts[1]
            
            if (period == "PM" && hour != 12) {
                hour += 12
            } else if (period == "AM" && hour == 12) {
                hour = 0
            }
            
            String.format("%02d:%02d", hour, minute)
        } catch (e: Exception) {
            time12
        }
    }
    
    private fun setupDatePickers() {
        binding.startDateButton.setOnClickListener {
            showDatePicker(startDate) { date ->
                startDate = date
                binding.startDateButton.text = date ?: "Select Start Date"
            }
        }
        
        binding.endDateButton.setOnClickListener {
            showDatePicker(endDate) { date ->
                endDate = date
                binding.endDateButton.text = date ?: "Select End Date"
            }
        }
        
        binding.clearStartDateButton.setOnClickListener {
            startDate = null
            binding.startDateButton.text = "Select Start Date"
        }
        
        binding.clearEndDateButton.setOnClickListener {
            endDate = null
            binding.endDateButton.text = "Select End Date"
        }
    }
    
    private fun setupContentSelection() {
        binding.selectContentButton.setOnClickListener {
            val fragment = ContentSelectionFragment.newInstance { type, id, name ->
                selectedContentType = type
                selectedContentId = id
                selectedContentName = name
                updateContentDisplay()
            }
            fragment.show(parentFragmentManager, "content_selection")
        }
    }
    
    private fun setupDisplaySelection() {
        binding.selectDisplaysButton.setOnClickListener {
            val fragment = DisplaySelectionFragment.newInstance(selectedDisplayIds) { displayIds ->
                selectedDisplayIds.clear()
                selectedDisplayIds.addAll(displayIds)
                updateDisplayCount()
            }
            fragment.show(parentFragmentManager, "display_selection")
        }
    }
    
    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveSchedule()
        }
        
        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(time)
            },
            hour,
            minute,
            false // Use 12-hour format with AM/PM
        ).show()
    }
    
    private fun showDatePicker(currentDate: String?, onDateSelected: (String?) -> Unit) {
        val calendar = Calendar.getInstance()
        if (currentDate != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = sdf.parse(currentDate) ?: Date()
            } catch (e: Exception) {
                // Use current date
            }
        }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun updateContentDisplay() {
        val text = when (selectedContentType) {
            "playlist" -> "Playlist: $selectedContentName"
            "layout" -> "Layout: $selectedContentName"
            else -> "Select Content"
        }
        binding.selectContentButton.text = text
    }
    
    private fun updateDisplayCount() {
        binding.selectDisplaysButton.text = "${selectedDisplayIds.size} display(s) selected"
    }
    
    private fun saveSchedule() {
        val name = binding.nameEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val priority = binding.priorityEditText.text.toString().toIntOrNull() ?: 1
        
        // Validation
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a schedule name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedDays.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedDisplayIds.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one display", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedContentId == null) {
            Toast.makeText(requireContext(), "Please select content (playlist or layout)", Toast.LENGTH_SHORT).show()
            return
        }
        
        val request = CreateScheduleRequest(
            name = name,
            description = description.ifEmpty { null },
            startTime = startTime,
            endTime = endTime,
            timezone = "Asia/Kolkata",
            repeatDays = selectedDays,
            startDate = startDate,
            endDate = endDate,
            priority = priority,
            playlistId = if (selectedContentType == "playlist") selectedContentId else null,
            layoutId = if (selectedContentType == "layout") selectedContentId else null,
            displayIds = selectedDisplayIds,
            orientation = null
        )
        
        if (scheduleId != null) {
            // Update existing schedule
            val updateRequest = UpdateScheduleRequest(
                name = name,
                description = description.ifEmpty { null },
                startTime = startTime,
                endTime = endTime,
                timezone = "Asia/Kolkata",
                repeatDays = selectedDays,
                startDate = startDate,
                endDate = endDate,
                priority = priority,
                playlistId = if (selectedContentType == "playlist") selectedContentId else null,
                layoutId = if (selectedContentType == "layout") selectedContentId else null,
                displayIds = selectedDisplayIds,
                isActive = null,
                orientation = null
            )
            viewModel.updateSchedule(scheduleId!!, updateRequest)
        } else {
            // Create new schedule
            viewModel.createSchedule(request) {
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe current schedule for editing
        viewModel.currentSchedule.observe(viewLifecycleOwner) { schedule ->
            schedule?.let {
                loadScheduleData(it)
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_SCHEDULE_ID = "schedule_id"
        
        fun newInstance(scheduleId: String?) = ScheduleEditorFragment().apply {
            arguments = bundleOf(ARG_SCHEDULE_ID to scheduleId)
        }
    }
}
