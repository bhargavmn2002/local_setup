package com.signox.offlinemanager.data.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType

@Dao
interface MediaDao {
    
    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    fun getAllMedia(): LiveData<List<Media>>
    
    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    suspend fun getAllMediaSync(): List<Media>
    
    @Query("SELECT * FROM media WHERE type = :type ORDER BY createdAt DESC")
    fun getMediaByType(type: MediaType): LiveData<List<Media>>
    
    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: Long): Media?
    
    @Query("SELECT * FROM media WHERE name LIKE :searchQuery ORDER BY createdAt DESC")
    fun searchMedia(searchQuery: String): LiveData<List<Media>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: Media): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(mediaList: List<Media>)
    
    @Update
    suspend fun updateMedia(media: Media)
    
    @Delete
    suspend fun deleteMedia(media: Media)
    
    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)
    
    @Query("SELECT COUNT(*) FROM media")
    suspend fun getMediaCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM media")
    suspend fun getTotalMediaSize(): Long?
}