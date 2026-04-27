package com.signox.offlinemanager.data.database

import androidx.room.TypeConverter
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.data.model.ContentType

class Converters {
    
    @TypeConverter
    fun fromMediaType(mediaType: MediaType): String {
        return mediaType.name
    }
    
    @TypeConverter
    fun toMediaType(mediaType: String): MediaType {
        return MediaType.valueOf(mediaType)
    }
    
    @TypeConverter
    fun fromContentType(contentType: ContentType): String {
        return contentType.name
    }
    
    @TypeConverter
    fun toContentType(contentType: String): ContentType {
        return ContentType.valueOf(contentType)
    }
}