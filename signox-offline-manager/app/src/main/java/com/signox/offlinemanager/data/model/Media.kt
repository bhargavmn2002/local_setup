package com.signox.offlinemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class Media(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val type: MediaType,
    val duration: Long = 0, // in milliseconds, 0 for images
    val fileSize: Long,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MediaType {
    IMAGE, VIDEO, AUDIO
}