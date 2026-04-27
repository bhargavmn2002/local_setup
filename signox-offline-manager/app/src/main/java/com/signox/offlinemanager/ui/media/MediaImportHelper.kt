package com.signox.offlinemanager.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MediaImportHelper(private val context: Context) {
    
    private val mediaDir = File(context.filesDir, "media")
    private val thumbnailDir = File(context.filesDir, "thumbnails")
    
    init {
        // Create directories if they don't exist
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }
    }
    
    suspend fun importMedia(uri: Uri): Media? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: return@withContext null
            val mimeType = getMimeType(uri) ?: return@withContext null
            val mediaType = getMediaTypeFromMimeType(mimeType) ?: return@withContext null
            
            // Copy file to internal storage
            val destinationFile = File(mediaDir, generateUniqueFileName(fileName))
            copyFile(uri, destinationFile)
            
            val fileSize = destinationFile.length()
            var duration = 0L
            var thumbnailPath: String? = null
            
            // Get duration and generate thumbnail for videos
            if (mediaType == MediaType.VIDEO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destinationFile.absolutePath)
                    
                    // Get duration
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = durationStr?.toLongOrNull() ?: 0L
                    
                    // Generate thumbnail
                    val bitmap = retriever.getFrameAtTime(1000000) // 1 second
                    if (bitmap != null) {
                        thumbnailPath = saveThumbnail(fileName, bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Get duration for audio files
            if (mediaType == MediaType.AUDIO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destinationFile.absolutePath)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = durationStr?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            return@withContext Media(
                name = fileName,
                filePath = destinationFile.absolutePath,
                type = mediaType,
                duration = duration,
                fileSize = fileSize,
                thumbnailPath = thumbnailPath
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        return fileName
    }
    
    private fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
    
    private fun getMediaTypeFromMimeType(mimeType: String): MediaType? {
        return when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("audio/") -> MediaType.AUDIO
            else -> null
        }
    }
    
    private fun generateUniqueFileName(originalName: String): String {
        val timestamp = System.currentTimeMillis()
        val extension = getFileExtension(originalName)
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        
        return "${nameWithoutExtension}_${timestamp}${extension}"
    }
    
    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot != -1) fileName.substring(lastDot) else ""
    }
    
    private fun copyFile(sourceUri: Uri, destinationFile: File) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    
    private fun saveThumbnail(originalFileName: String, bitmap: Bitmap): String? {
        return try {
            val thumbnailFileName = "thumb_${generateUniqueFileName(originalFileName)}.jpg"
            val thumbnailFile = File(thumbnailDir, thumbnailFileName)
            
            FileOutputStream(thumbnailFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            
            thumbnailFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun getSupportedMimeTypes(): Array<String> {
        return arrayOf(
            "image/*",
            "video/*",
            "audio/*"
        )
    }
}