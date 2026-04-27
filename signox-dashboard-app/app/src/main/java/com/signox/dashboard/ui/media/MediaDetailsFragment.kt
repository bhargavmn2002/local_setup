package com.signox.dashboard.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.databinding.FragmentMediaDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MediaDetailsFragment : Fragment() {
    
    private var _binding: FragmentMediaDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaViewModel by viewModels()
    private var media: Media? = null
    
    companion object {
        private const val ARG_MEDIA = "media"
        
        fun newInstance(media: Media): MediaDetailsFragment {
            return MediaDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MEDIA, media)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = arguments?.getParcelable(ARG_MEDIA)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        media?.let { displayMediaDetails(it) }
        setupButtons()
        observeViewModel()
    }
    
    private fun displayMediaDetails(media: Media) {
        // Load thumbnail
        if (media.type == "IMAGE") {
            Glide.with(this)
                .load(media.url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivThumbnail)
        } else {
            // For videos, show video icon or thumbnail if available
            binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Display details
        binding.tvFileName.text = media.originalName ?: media.name
        binding.tvFileType.text = media.type
        binding.tvFileSize.text = media.formattedSize
        binding.tvUploadDate.text = formatDate(media.createdAt)
        
        // Display expiry date
        if (media.endDate != null) {
            binding.tvExpiryDate.text = formatDate(media.endDate)
            binding.tvExpiryDate.visibility = View.VISIBLE
            binding.tvExpiryDateLabel.visibility = View.VISIBLE
        } else {
            binding.tvExpiryDate.text = "Not set"
            binding.tvExpiryDate.visibility = View.VISIBLE
            binding.tvExpiryDateLabel.visibility = View.VISIBLE
        }
        
        // Display dimensions for images
        if (media.type == "IMAGE" && media.width != null && media.height != null) {
            binding.tvDimensions.text = "${media.width} x ${media.height}"
            binding.tvDimensions.visibility = View.VISIBLE
            binding.tvDimensionsLabel.visibility = View.VISIBLE
        } else {
            binding.tvDimensions.visibility = View.GONE
            binding.tvDimensionsLabel.visibility = View.GONE
        }
        
        // Display duration for videos
        if (media.type == "VIDEO" && media.duration != null) {
            binding.tvDuration.text = media.formattedDuration ?: "N/A"
            binding.tvDuration.visibility = View.VISIBLE
            binding.tvDurationLabel.visibility = View.VISIBLE
        } else {
            binding.tvDuration.visibility = View.GONE
            binding.tvDurationLabel.visibility = View.GONE
        }
        
        // Display URL
        binding.tvUrl.text = media.url
    }
    
    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            showEditDialog()
        }
        
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnPreview.setOnClickListener {
            media?.let { media ->
                val fragment = MediaPreviewFragment.newInstance(media)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.selectedMedia.observe(viewLifecycleOwner) { updatedMedia ->
            updatedMedia?.let {
                media = it
                displayMediaDetails(it)
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
                
                // If deleted, go back
                if (it.contains("deleted", ignoreCase = true)) {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun showEditDialog() {
        val currentMedia = media ?: return
        
        val options = arrayOf("Edit Name", "Set Expiry Date", "Remove Expiry Date")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Media")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditNameDialog()
                    1 -> showSetExpiryDateDialog()
                    2 -> removeExpiryDate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditNameDialog() {
        val currentMedia = media ?: return
        
        val input = android.widget.EditText(requireContext())
        input.setText(currentMedia.originalName ?: currentMedia.name)
        input.hint = "Enter new name"
        
        // Add padding
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Media Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != (currentMedia.originalName ?: currentMedia.name)) {
                    viewModel.updateMedia(currentMedia.id, name = newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSetExpiryDateDialog() {
        val currentMedia = media ?: return
        
        val calendar = Calendar.getInstance()
        
        // If there's an existing expiry date, use it
        currentMedia.endDate?.let { dateString ->
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(dateString)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }
        
        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Format the date as ISO 8601
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59)
                
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val expiryDate = format.format(selectedCalendar.time)
                
                // Update media with expiry date
                viewModel.updateMediaWithExpiry(currentMedia.id, expiryDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    private fun removeExpiryDate() {
        val currentMedia = media ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Expiry Date")
            .setMessage("Are you sure you want to remove the expiry date? This media will not expire.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.updateMediaWithExpiry(currentMedia.id, null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog() {
        val currentMedia = media ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Media")
            .setMessage("Are you sure you want to delete \"${currentMedia.originalName}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMedia(currentMedia.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)
            
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
