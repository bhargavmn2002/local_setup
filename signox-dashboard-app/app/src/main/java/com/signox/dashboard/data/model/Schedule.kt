package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class Schedule(
    val id: String,
    val name: String,
    val description: String?,
    val startTime: String,
    val endTime: String,
    val timezone: String,
    val repeatDays: List<String>,
    val startDate: String?,
    val endDate: String?,
    val priority: Int,
    val playlistId: String?,
    val layoutId: String?,
    val orientation: String?,
    val isActive: Boolean,
    val createdById: String,
    val createdAt: String,
    val updatedAt: String,
    val playlist: Playlist?,
    val layout: Layout?,
    val displays: List<ScheduleDisplay>?,
    val createdBy: User?
)

data class ScheduleDisplay(
    val id: String,
    val scheduleId: String,
    val displayId: String,
    val display: Display?
)

data class ScheduleListResponse(
    val schedules: List<Schedule>
)

data class ScheduleResponse(
    val schedule: Schedule,
    val message: String?
)

data class CreateScheduleRequest(
    val name: String,
    val description: String?,
    val startTime: String,
    val endTime: String,
    val timezone: String = "Asia/Kolkata",
    val repeatDays: List<String>,
    val startDate: String?,
    val endDate: String?,
    val priority: Int = 1,
    val playlistId: String?,
    val layoutId: String?,
    val displayIds: List<String>,
    val orientation: String?
)

data class UpdateScheduleRequest(
    val name: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val timezone: String?,
    val repeatDays: List<String>?,
    val startDate: String?,
    val endDate: String?,
    val priority: Int?,
    val playlistId: String?,
    val layoutId: String?,
    val displayIds: List<String>?,
    val isActive: Boolean?,
    val orientation: String?
)

data class ActiveSchedulesResponse(
    val activeSchedules: List<Schedule>,
    val currentTime: String,
    val currentDay: String,
    val timezone: String
)

// Helper data class for day selection
data class DayOfWeek(
    val name: String,
    val displayName: String,
    var isSelected: Boolean = false
) {
    companion object {
        fun getAll(): List<DayOfWeek> {
            return listOf(
                DayOfWeek("monday", "Monday"),
                DayOfWeek("tuesday", "Tuesday"),
                DayOfWeek("wednesday", "Wednesday"),
                DayOfWeek("thursday", "Thursday"),
                DayOfWeek("friday", "Friday"),
                DayOfWeek("saturday", "Saturday"),
                DayOfWeek("sunday", "Sunday")
            )
        }
    }
}
