package com.signox.offlinemanager.ui.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.MediaRepository

class LayoutEditorViewModelFactory(
    private val layoutRepository: LayoutRepository,
    private val mediaRepository: MediaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LayoutEditorViewModel::class.java)) {
            return LayoutEditorViewModel(layoutRepository, mediaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}