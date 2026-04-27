package com.signox.dashboard.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Media(
    val id: String,
    val name: String,
    val originalName: String?,
    val filename: String?,
    @SerializedName("type") val type: String,
    val url: String,
    val duration: Int?,
    val fileSize: Int?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val description: String?,
    val tags: List<String>?,
    val endDate: String?,
    val createdAt: String,
    val updatedAt: String,
    val createdById: String?,
    @SerializedName("createdBy") val uploadedBy: MediaUser?
) : Parcelable {
    val isImage: Boolean
        get() = type == "IMAGE" || mimeType?.startsWith("image/") == true
    
    val isVideo: Boolean
        get() = type == "VIDEO" || mimeType?.startsWith("video/") == true
    
    val sizeInMB: Double
        get() = (fileSize ?: 0) / (1024.0 * 1024.0)
    
    val formattedSize: String
        get() {
            val size = fileSize ?: 0
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            }
        }
    
    val formattedDuration: String?
        get() = duration?.let {
            val minutes = (it / 60).toInt()
            val seconds = (it % 60).toInt()
            String.format("%d:%02d", minutes, seconds)
        }
    
    // Thumbnail URLs (backend generates these for images)
    val thumbnailUrl: String?
        get() = if (isImage && filename != null) {
            val baseFilename = filename.substringBeforeLast(".")
            "/uploads/thumbnails/${baseFilename}_thumb.jpg"
        } else null
    
    val optimizedUrl: String?
        get() = if (isImage && filename != null) {
            val baseFilename = filename.substringBeforeLast(".")
            "/uploads/optimized/${baseFilename}_optimized.jpg"
        } else null
    
    val previewUrl: String?
        get() = if (isImage && filename != null) {
            val baseFilename = filename.substringBeforeLast(".")
            "/uploads/optimized/${baseFilename}_preview.jpg"
        } else null
    
    val hlsUrl: String?
        get() = if (isVideo && url.contains("/hls/") && url.endsWith("/index.m3u8")) {
            url
        } else null
}

// Simplified User model for media responses (to avoid conflict with main User model)
@Parcelize
data class MediaUser(
    val id: String,
    val email: String,
    val role: String
) : Parcelable

data class MediaListResponse(
    @SerializedName("data") val data: List<Media>?,
    @SerializedName("media") val media: List<Media>?,
    val pagination: PaginationInfo?,
    val total: Int?,
    val page: Int?,
    val totalPages: Int?,
    val storageInfo: StorageInfo?
) {
    // Helper to get media list regardless of which field is used
    val mediaList: List<Media>
        get() = media ?: data ?: emptyList()
}

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val itemsPerPage: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean,
    val nextPage: Int?,
    val prevPage: Int?
) {
    // Compatibility properties
    val total: Int get() = totalItems
    val page: Int get() = currentPage
    val limit: Int get() = itemsPerPage
}

data class StorageInfo(
    val limitMB: Long? = null,
    val usedBytes: Long,
    val usedMB: Double? = null,
    val availableBytes: Long? = null,
    val availableMB: Double? = null,
    // Legacy fields for backward compatibility
    val used: Long? = null,
    val limit: Long? = null,
    val percentage: Double? = null,
    val remaining: Long? = null
) {
    val usedInMB: Double
        get() = usedMB ?: (usedBytes / (1024.0 * 1024.0))
    
    val limitInMB: Double
        get() = limitMB?.toDouble() ?: (limit?.let { it / (1024.0 * 1024.0) } ?: 0.0)
    
    val remainingInMB: Double
        get() = availableMB ?: (availableBytes?.let { it / (1024.0 * 1024.0) } ?: 0.0)
    
    val calculatedPercentage: Double
        get() = percentage ?: if (limitMB != null && limitMB > 0) {
            (usedInMB / limitMB) * 100
        } else {
            0.0
        }
    
    val formattedUsed: String
        get() = formatBytes(usedBytes)
    
    val formattedLimit: String
        get() = limitMB?.let { "${it}MB" } ?: formatBytes(limit ?: 0)
    
    val formattedRemaining: String
        get() = availableBytes?.let { formatBytes(it) } ?: formatBytes(remaining ?: 0)
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

data class StorageInfoResponse(
    val storage: StorageInfo
)

data class MediaUploadResponse(
    val media: Media,
    val message: String,
    val storageInfo: StorageInfo?
)

data class MediaDeleteResponse(
    val message: String,
    val storageInfo: StorageInfo?
)
