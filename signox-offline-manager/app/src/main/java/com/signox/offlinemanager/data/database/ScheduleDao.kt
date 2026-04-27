package com.signox.offlinemanager.data.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.data.model.ContentType

@Dao
interface ScheduleDao {
    
    @Query("SELECT * FROM schedules ORDER BY startTime ASC")
    fun getAllSchedules(): LiveData<List<Schedule>>
    
    @Query("SELECT * FROM schedules ORDER BY startTime ASC")
    suspend fun getAllSchedulesSync(): List<Schedule>
    
    @Query("SELECT * FROM schedules WHERE isActive = 1 ORDER BY startTime ASC")
    fun getActiveSchedules(): LiveData<List<Schedule>>
    
    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): Schedule?
    
    @Query("SELECT * FROM schedules WHERE contentType = :contentType AND contentId = :contentId")
    fun getSchedulesForContent(contentType: ContentType, contentId: Long): LiveData<List<Schedule>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long
    
    @Update
    suspend fun updateSchedule(schedule: Schedule)
    
    @Delete
    suspend fun deleteSchedule(schedule: Schedule)
    
    @Query("UPDATE schedules SET isActive = :isActive WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, isActive: Boolean)
    
    @Query("SELECT COUNT(*) FROM schedules WHERE isActive = 1")
    suspend fun getActiveScheduleCount(): Int
}