package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    suspend fun getSchedules(): Result<List<Schedule>> {
        return try {
            val response = apiService.getSchedules()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.schedules)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch schedules"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createSchedule(request: CreateScheduleRequest): Result<Schedule> {
        return try {
            val response = apiService.createSchedule(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.schedule)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSchedule(id: String, request: UpdateScheduleRequest): Result<Schedule> {
        return try {
            val response = apiService.updateSchedule(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.schedule)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteSchedule(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteSchedule(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActiveSchedules(displayId: String? = null, timezone: String = "Asia/Kolkata"): Result<ActiveSchedulesResponse> {
        return try {
            val response = apiService.getActiveSchedules(displayId, timezone)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch active schedules"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
