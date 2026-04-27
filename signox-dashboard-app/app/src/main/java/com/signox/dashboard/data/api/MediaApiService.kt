package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MediaApiService {
    
    @GET("media")
    suspend fun getMedia(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("type") type: String? = null,
        @Header("Cache-Control") cacheControl: String = "no-cache"
    ): Response<MediaListResponse>
    
    @Multipart
    @POST("media")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody? = null
    ): Response<MediaUploadResponse>
    
    @PUT("media/{id}")
    suspend fun updateMedia(
        @Path("id") id: String,
        @Body request: UpdateMediaRequest
    ): Response<MediaUpdateResponse>
    
    @DELETE("media/{id}")
    suspend fun deleteMedia(@Path("id") id: String): Response<MediaDeleteResponse>
    
    @GET("media/storage-info")
    suspend fun getStorageInfo(): Response<StorageInfoResponse>
}

data class UpdateMediaRequest(
    val name: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val endDate: String? = null
)

data class MediaUpdateResponse(
    val media: Media
)
