package com.signox.offlinemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val startTime: String, // HH:mm format
    val endTime: String, // HH:mm format
    val daysOfWeek: String, // Comma-separated: "1,2,3,4,5" (Monday=1, Sunday=7)
    val contentType: ContentType,
    val contentId: Long, // playlistId or layoutId
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ContentType {
    PLAYLIST, LAYOUT
}