package com.signox.offlinemanager.ui.schedule

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.ContentType
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.databinding.DialogCreateScheduleBinding
import java.util.*

class CreateScheduleDialog(
    private val onCreateSchedule: (String, String?, String, String, String, ContentType, Long) -> Unit,
    private val playlists: List<Playlist>,
    private val layouts: List<Layout>,
    private val existingSchedule: Schedule? = null
) : DialogFragment() {
    
    private var _binding: DialogCreateScheduleBinding? = null
    private val binding get() = _binding!!
    
    private val selectedDays = mutableSetOf<Int>()
    private var selectedContentType = ContentType.PLAYLIST
    private var selectedContentId = -1L
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateScheduleBinding.inflate(layoutInflater)
        
        setupViews()
        
        val title = if (existingSchedule != null) "Edit Schedule" else "Create New Schedule"
        val positiveButtonText = if (existingSchedule != null) "Update" else "Create"
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                createSchedule()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupViews() {
        setupTimeSelectors()
        setupDaySelectors()
        setupContentSelectors()
        
        // Pre-fill if editing
        existingSchedule?.let { schedule ->
            binding.editTextScheduleName.setText(schedule.name)
            binding.editTextScheduleDescription.setText(schedule.description)
            binding.textStartTime.text = schedule.startTime
            binding.textEndTime.text = schedule.endTime
            
            // Set selected days
            schedule.daysOfWeek.split(",").forEach { day ->
                selectedDays.add(day.toInt())
            }
            updateDayButtons()
            
            // Set content type and selection
            selectedContentType = schedule.contentType
            selectedContentId = schedule.contentId
            updateContentSpinner()
        }
    }
    
    private fun setupTimeSelectors() {
        binding.buttonStartTime.setOnClickListener {
            showTimePicker { hour, minute ->
                val timeString = String.format("%02d:%02d", hour, minute)
                binding.textStartTime.text = timeString
            }
        }
        
        binding.buttonEndTime.setOnClickListener {
            showTimePicker { hour, minute ->
                val timeString = String.format("%02d:%02d", hour, minute)
                binding.textEndTime.text = timeString
            }
        }
        
        // Set default times if creating new schedule
        if (existingSchedule == null) {
            binding.textStartTime.text = "09:00"
            binding.textEndTime.text = "17:00"
        }
    }
    
    private fun setupDaySelectors() {
        val dayButtons = listOf(
            binding.buttonMonday to 1,
            binding.buttonTuesday to 2,
            binding.buttonWednesday to 3,
            binding.buttonThursday to 4,
            binding.buttonFriday to 5,
            binding.buttonSaturday to 6,
            binding.buttonSunday to 7
        )
        
        dayButtons.forEach { (button, dayNumber) ->
            button.setOnClickListener {
                if (selectedDays.contains(dayNumber)) {
                    selectedDays.remove(dayNumber)
                } else {
                    selectedDays.add(dayNumber)
                }
                updateDayButtons()
            }
        }
        
        // Select weekdays by default for new schedules
        if (existingSchedule == null) {
            selectedDays.addAll(listOf(1, 2, 3, 4, 5))
            updateDayButtons()
        }
    }
    
    private fun setupContentSelectors() {
        // Content type radio buttons
        binding.radioGroupContentType.setOnCheckedChangeListener { _, checkedId ->
            selectedContentType = when (checkedId) {
                R.id.radio_playlist -> ContentType.PLAYLIST
                R.id.radio_layout -> ContentType.LAYOUT
                else -> ContentType.PLAYLIST
            }
            updateContentSpinner()
        }
        
        updateContentSpinner()
    }
    
    private fun updateDayButtons() {
        val dayButtons = listOf(
            binding.buttonMonday to 1,
            binding.buttonTuesday to 2,
            binding.buttonWednesday to 3,
            binding.buttonThursday to 4,
            binding.buttonFriday to 5,
            binding.buttonSaturday to 6,
            binding.buttonSunday to 7
        )
        
        dayButtons.forEach { (button, dayNumber) ->
            button.isSelected = selectedDays.contains(dayNumber)
            button.setBackgroundColor(
                if (button.isSelected) {
                    requireContext().getColor(R.color.primary)
                } else {
                    requireContext().getColor(R.color.card_background)
                }
            )
        }
    }
    
    private fun updateContentSpinner() {
        val items = when (selectedContentType) {
            ContentType.PLAYLIST -> playlists.map { "${it.name} (Playlist)" }
            ContentType.LAYOUT -> layouts.map { "${it.name} (Layout)" }
        }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerContent.adapter = adapter
        
        // Set selection if editing
        if (existingSchedule != null && existingSchedule.contentType == selectedContentType) {
            val index = when (selectedContentType) {
                ContentType.PLAYLIST -> playlists.indexOfFirst { it.id == existingSchedule.contentId }
                ContentType.LAYOUT -> layouts.indexOfFirst { it.id == existingSchedule.contentId }
            }
            if (index != -1) {
                binding.spinnerContent.setSelection(index)
            }
        }
    }
    
    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                onTimeSelected(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true
        ).show()
    }
    
    private fun createSchedule() {
        val name = binding.editTextScheduleName.text.toString().trim()
        val description = binding.editTextScheduleDescription.text.toString().trim()
        val startTime = binding.textStartTime.text.toString()
        val endTime = binding.textEndTime.text.toString()
        
        if (name.isEmpty()) {
            binding.editTextScheduleName.error = "Schedule name is required"
            return
        }
        
        if (selectedDays.isEmpty()) {
            // Show error for days selection
            return
        }
        
        val selectedPosition = binding.spinnerContent.selectedItemPosition
        if (selectedPosition == -1) {
            // Show error for content selection
            return
        }
        
        val contentId = when (selectedContentType) {
            ContentType.PLAYLIST -> playlists[selectedPosition].id
            ContentType.LAYOUT -> layouts[selectedPosition].id
        }
        
        val daysOfWeek = selectedDays.sorted().joinToString(",")
        
        onCreateSchedule(
            name,
            description.ifEmpty { null },
            startTime,
            endTime,
            daysOfWeek,
            selectedContentType,
            contentId
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}