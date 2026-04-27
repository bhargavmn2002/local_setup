package com.signox.offlinemanager.ui.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signox.offlinemanager.data.repository.LayoutRepository

class LayoutViewModelFactory(
    private val layoutRepository: LayoutRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LayoutViewModel::class.java)) {
            return LayoutViewModel(layoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}