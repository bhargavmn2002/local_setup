package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.MediaApiService
import com.signox.dashboard.data.api.UpdateMediaRequest
import com.signox.dashboard.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val apiService: MediaApiService
) {
    
    suspend fun getMedia(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null,
        type: String? = null
    ): Result<MediaListResponse> {
        return try {
            android.util.Log.d("MediaRepository", "Fetching media: page=$page, limit=$limit, search=$search, type=$type")
            
            val response = apiService.getMedia(page, limit, search, type)
            
            android.util.Log.d("MediaRepository", "Response code: ${response.code()}")
            android.util.Log.d("MediaRepository", "Response headers: ${response.headers()}")
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                android.util.Log.d("MediaRepository", "Success! Media count: ${body.mediaList.size}")
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("MediaRepository", "API Error: ${response.code()}")
                android.util.Log.e("MediaRepository", "Error body: $errorBody")
                
                // Check if error body is JSON or plain text
                val errorMessage = if (errorBody != null) {
                    try {
                        val jsonError = org.json.JSONObject(errorBody)
                        jsonError.optString("message", "Failed to fetch media")
                    } catch (e: Exception) {
                        // Not JSON, use as-is
                        errorBody.take(200)
                    }
                } else {
                    "Failed to fetch media: ${response.code()}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("MediaRepository", "JSON parsing error", e)
            android.util.Log.e("MediaRepository", "This usually means the server returned unexpected data format")
            Result.failure(Exception("Server returned invalid data format. Please try again."))
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Exception fetching media", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadMedia(file: File, name: String? = null): Result<MediaUploadResponse> {
        return try {
            // Determine MIME type from file extension
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mov" -> "video/quicktime"
                "avi" -> "video/x-msvideo"
                else -> "application/octet-stream"
            }
            
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val namePart = name?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.uploadMedia(filePart, namePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null && errorBody.contains("message")) {
                    try {
                        val jsonError = org.json.JSONObject(errorBody)
                        jsonError.getString("message")
                    } catch (e: Exception) {
                        response.message() ?: "Failed to upload media"
                    }
                } else {
                    response.message() ?: "Failed to upload media"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMedia(
        id: String,
        name: String? = null,
        description: String? = null,
        tags: List<String>? = null
    ): Result<Media> {
        return try {
            val request = UpdateMediaRequest(name, description, tags)
            val response = apiService.updateMedia(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.media)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMediaExpiry(
        id: String,
        endDate: String?
    ): Result<Media> {
        return try {
            val request = UpdateMediaRequest(endDate = endDate)
            val response = apiService.updateMedia(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.media)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update expiry date"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMedia(id: String): Result<String> {
        return try {
            val response = apiService.deleteMedia(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStorageInfo(): Result<StorageInfo> {
        return try {
            val response = apiService.getStorageInfo()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.storage)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch storage info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
