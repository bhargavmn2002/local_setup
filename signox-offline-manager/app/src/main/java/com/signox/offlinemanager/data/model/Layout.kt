package com.signox.offlinemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layouts")
data class Layout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val backgroundColor: String = "#000000",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "layout_zones")
data class LayoutZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val layoutId: Long,
    val name: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val zIndex: Int = 0,
    val mediaId: Long? = null,
    val playlistId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)