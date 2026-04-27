package com.signox.offlinemanager.data.repository

import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.database.LayoutDao
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.LayoutZone

class LayoutRepository(
    private val layoutDao: LayoutDao
) {
    
    fun getAllLayouts(): LiveData<List<Layout>> = layoutDao.getAllLayouts()
    
    fun getAllLayoutsLiveData(): LiveData<List<Layout>> = layoutDao.getAllLayouts()
    
    suspend fun getLayoutById(id: Long): Layout? = layoutDao.getLayoutById(id)
    
    fun getLayoutZonesLiveData(layoutId: Long): LiveData<List<LayoutZone>> = 
        layoutDao.getLayoutZones(layoutId)
    
    suspend fun getLayoutZones(layoutId: Long): List<LayoutZone> = 
        layoutDao.getLayoutZonesSync(layoutId)
    
    suspend fun createLayout(layout: Layout): Long = layoutDao.insertLayout(layout)
    
    suspend fun updateLayout(layout: Layout) = layoutDao.updateLayout(layout)
    
    suspend fun deleteLayout(layout: Layout) {
        // Delete all zones first, then the layout
        layoutDao.deleteLayoutZones(layout.id)
        layoutDao.deleteLayout(layout)
    }
    
    suspend fun createLayoutZone(layoutZone: LayoutZone): Long = 
        layoutDao.insertLayoutZone(layoutZone)
    
    suspend fun updateLayoutZone(layoutZone: LayoutZone) = 
        layoutDao.updateLayoutZone(layoutZone)
    
    suspend fun deleteLayoutZone(layoutZone: LayoutZone) = 
        layoutDao.deleteLayoutZone(layoutZone)
    
    suspend fun saveLayoutWithZones(layout: Layout, zones: List<LayoutZone>): Long {
        val layoutId = layoutDao.insertLayout(layout)
        val zonesWithLayoutId = zones.map { it.copy(layoutId = layoutId) }
        layoutDao.insertLayoutZones(zonesWithLayoutId)
        return layoutId
    }
    
    suspend fun updateLayoutWithZones(layout: Layout, zones: List<LayoutZone>) {
        layoutDao.updateLayout(layout)
        // Delete existing zones and insert new ones
        layoutDao.deleteLayoutZones(layout.id)
        layoutDao.insertLayoutZones(zones)
    }
    
    suspend fun getLayoutCount(): Int = layoutDao.getLayoutCount()
}