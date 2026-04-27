package com.signox.offlinemanager.data.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.LayoutZone

@Dao
interface LayoutDao {
    
    @Query("SELECT * FROM layouts ORDER BY createdAt DESC")
    fun getAllLayouts(): LiveData<List<Layout>>
    
    @Query("SELECT * FROM layouts WHERE id = :id")
    suspend fun getLayoutById(id: Long): Layout?
    
    @Query("SELECT * FROM layout_zones WHERE layoutId = :layoutId ORDER BY zIndex ASC")
    fun getLayoutZones(layoutId: Long): LiveData<List<LayoutZone>>
    
    @Query("SELECT * FROM layout_zones WHERE layoutId = :layoutId ORDER BY zIndex ASC")
    suspend fun getLayoutZonesSync(layoutId: Long): List<LayoutZone>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: Layout): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayoutZone(layoutZone: LayoutZone): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayoutZones(layoutZones: List<LayoutZone>)
    
    @Update
    suspend fun updateLayout(layout: Layout)
    
    @Update
    suspend fun updateLayoutZone(layoutZone: LayoutZone)
    
    @Delete
    suspend fun deleteLayout(layout: Layout)
    
    @Delete
    suspend fun deleteLayoutZone(layoutZone: LayoutZone)
    
    @Query("DELETE FROM layout_zones WHERE layoutId = :layoutId")
    suspend fun deleteLayoutZones(layoutId: Long)
    
    @Query("SELECT COUNT(*) FROM layouts")
    suspend fun getLayoutCount(): Int
}