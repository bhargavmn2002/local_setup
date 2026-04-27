package com.signox.offlinemanager.data.repository

import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.database.ScheduleDao
import com.signox.offlinemanager.data.model.ContentType
import com.signox.offlinemanager.data.model.Schedule

class ScheduleRepository(
    private val scheduleDao: ScheduleDao
) {
    
    fun getAllSchedulesLiveData(): LiveData<List<Schedule>> = scheduleDao.getAllSchedules()
    
    suspend fun getAllSchedules(): List<Schedule> = scheduleDao.getAllSchedulesSync()
    
    fun getActiveSchedules(): LiveData<List<Schedule>> = scheduleDao.getActiveSchedules()
    
    suspend fun getScheduleById(id: Long): Schedule? = scheduleDao.getScheduleById(id)
    
    fun getSchedulesForContent(contentType: ContentType, contentId: Long): LiveData<List<Schedule>> =
        scheduleDao.getSchedulesForContent(contentType, contentId)
    
    suspend fun createSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)
    
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    
    suspend fun toggleScheduleStatus(schedule: Schedule) {
        val updatedSchedule = schedule.copy(
            isActive = !schedule.isActive,
            updatedAt = System.currentTimeMillis()
        )
        scheduleDao.updateSchedule(updatedSchedule)
    }
    
    suspend fun getActiveScheduleCount(): Int = scheduleDao.getActiveScheduleCount()
    
    // Helper method to check for schedule conflicts
    suspend fun hasConflict(
        startTime: String,
        endTime: String,
        daysOfWeek: String,
        excludeScheduleId: Long? = null
    ): Boolean {
        val allSchedules = getAllSchedules()
        val newDays = daysOfWeek.split(",").map { it.toInt() }.toSet()
        
        return allSchedules.any { schedule ->
            if (excludeScheduleId != null && schedule.id == excludeScheduleId) return@any false
            if (!schedule.isActive) return@any false
            
            val existingDays = schedule.daysOfWeek.split(",").map { it.toInt() }.toSet()
            val hasCommonDays = newDays.intersect(existingDays).isNotEmpty()
            
            if (hasCommonDays) {
                val newStart = timeToMinutes(startTime)
                val newEnd = timeToMinutes(endTime)
                val existingStart = timeToMinutes(schedule.startTime)
                val existingEnd = timeToMinutes(schedule.endTime)
                
                // Check for time overlap
                !(newEnd <= existingStart || newStart >= existingEnd)
            } else {
                false
            }
        }
    }
    
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}