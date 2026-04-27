package com.signox.offlinemanager.data.repository

import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.database.MediaDao
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType

class MediaRepository(private val mediaDao: MediaDao) {
    
    fun getAllMediaLive(): LiveData<List<Media>> = mediaDao.getAllMedia()
    
    fun getMediaByType(type: MediaType): LiveData<List<Media>> = mediaDao.getMediaByType(type)
    
    fun searchMedia(query: String): LiveData<List<Media>> = mediaDao.searchMedia("%$query%")
    
    suspend fun getAllMedia(): List<Media> = mediaDao.getAllMediaSync()
    
    suspend fun getMediaById(id: Long): Media? = mediaDao.getMediaById(id)
    
    suspend fun insertMedia(media: Media): Long = mediaDao.insertMedia(media)
    
    suspend fun insertAllMedia(mediaList: List<Media>) = mediaDao.insertAllMedia(mediaList)
    
    suspend fun updateMedia(media: Media) = mediaDao.updateMedia(media)
    
    suspend fun deleteMedia(media: Media) = mediaDao.deleteMedia(media)
    
    suspend fun deleteMediaById(id: Long) = mediaDao.deleteMediaById(id)
    
    suspend fun getMediaCount(): Int = mediaDao.getMediaCount()
    
    suspend fun getTotalMediaSize(): Long = mediaDao.getTotalMediaSize() ?: 0L
}