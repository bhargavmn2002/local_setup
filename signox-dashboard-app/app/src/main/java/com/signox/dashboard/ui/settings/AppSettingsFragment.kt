package com.signox.dashboard.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.dashboard.BuildConfig
import com.signox.dashboard.R
import com.signox.dashboard.databinding.FragmentAppSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AppSettingsFragment : Fragment() {
    
    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sharedPrefs: android.content.SharedPreferences
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", Context.MODE_PRIVATE)
        
        setupToolbar()
        loadSettings()
        setupListeners()
        updateCacheSize()
        updateAppInfo()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun loadSettings() {
        // Load notifications setting
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        binding.switchNotifications.isChecked = notificationsEnabled
        
        // Load theme setting
        val theme = sharedPrefs.getString("theme", "AUTO") ?: "AUTO"
        when (theme) {
            "LIGHT" -> binding.radioLight.isChecked = true
            "DARK" -> binding.radioDark.isChecked = true
            else -> binding.radioAuto.isChecked = true
        }
        
        // Load language setting
        val language = sharedPrefs.getString("language", "en") ?: "en"
        binding.btnLanguage.text = when (language) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            else -> "English"
        }
        
        // Load refresh interval
        val refreshInterval = sharedPrefs.getInt("refresh_interval", 30)
        binding.sliderRefreshInterval.value = refreshInterval.toFloat()
        binding.tvRefreshInterval.text = "$refreshInterval seconds"
    }
    
    private fun setupListeners() {
        // Notifications toggle
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(requireContext(), 
                if (isChecked) "Notifications enabled" else "Notifications disabled", 
                Toast.LENGTH_SHORT).show()
        }
        
        // Theme selection
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioLight -> "LIGHT"
                R.id.radioDark -> "DARK"
                else -> "AUTO"
            }
            sharedPrefs.edit().putString("theme", theme).apply()
            Toast.makeText(requireContext(), "Theme changed to $theme", Toast.LENGTH_SHORT).show()
            // Note: Actual theme change would require app restart or activity recreation
        }
        
        // Language selection
        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }
        
        // Refresh interval slider
        binding.sliderRefreshInterval.addOnChangeListener { _, value, _ ->
            val interval = value.toInt()
            binding.tvRefreshInterval.text = "$interval seconds"
            sharedPrefs.edit().putInt("refresh_interval", interval).apply()
        }
        
        // Clear cache button
        binding.btnClearCache.setOnClickListener {
            showClearCacheConfirmation()
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Spanish", "French")
        val languageCodes = arrayOf("en", "es", "fr")
        
        val currentLanguage = sharedPrefs.getString("language", "en") ?: "en"
        val currentIndex = languageCodes.indexOf(currentLanguage)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                sharedPrefs.edit().putString("language", selectedLanguage).apply()
                binding.btnLanguage.text = languages[which]
                Toast.makeText(requireContext(), "Language changed to ${languages[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showClearCacheConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Are you sure you want to clear the app cache? This will free up storage space.")
            .setPositiveButton("Clear") { _, _ ->
                clearCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearCache() {
        try {
            val cacheDir = requireContext().cacheDir
            deleteDir(cacheDir)
            updateCacheSize()
            Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to clear cache: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(dir, child))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }
    
    private fun updateCacheSize() {
        try {
            val cacheDir = requireContext().cacheDir
            val size = getDirSize(cacheDir)
            val sizeMB = size / (1024.0 * 1024.0)
            binding.tvCacheSize.text = String.format("Cache size: %.2f MB", sizeMB)
        } catch (e: Exception) {
            binding.tvCacheSize.text = "Cache size: 0 MB"
        }
    }
    
    private fun getDirSize(dir: File?): Long {
        var size: Long = 0
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            if (children != null) {
                for (child in children) {
                    size += if (child.isDirectory) {
                        getDirSize(child)
                    } else {
                        child.length()
                    }
                }
            }
        } else if (dir != null && dir.isFile) {
            size = dir.length()
        }
        return size
    }
    
    private fun updateAppInfo() {
        try {
            binding.tvAppVersion.text = "Version ${BuildConfig.VERSION_NAME}"
            binding.tvBuildNumber.text = "Build ${BuildConfig.VERSION_CODE}"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Version 1.0.0"
            binding.tvBuildNumber.text = "Build 1"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
