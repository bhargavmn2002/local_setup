package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("auth/me")
    suspend fun getMe(): Response<ProfileResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
    
    // User Profile
    @GET("users/profile")
    suspend fun getUserProfile(): Response<ProfileResponse>
    
    // Analytics Summary (Dashboard Stats)
    @GET("analytics/summary")
    suspend fun getAnalyticsSummary(): Response<DashboardSummary>
    
    // Playlists
    @GET("playlists")
    suspend fun getPlaylists(): Response<PlaylistsResponse>
    
    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: String): Response<PlaylistResponse>
    
    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<PlaylistResponse>
    
    @PUT("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: String,
        @Body request: UpdatePlaylistRequest
    ): Response<PlaylistResponse>
    
    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): Response<DeletePlaylistResponse>
    
    // Layouts
    @GET("layouts")
    suspend fun getLayouts(): Response<LayoutsResponse>
    
    @GET("layouts/{id}")
    suspend fun getLayout(@Path("id") id: String): Response<LayoutResponse>
    
    @POST("layouts")
    suspend fun createLayout(@Body request: CreateLayoutRequest): Response<LayoutResponse>
    
    @PUT("layouts/{id}")
    suspend fun updateLayout(
        @Path("id") id: String,
        @Body request: UpdateLayoutRequest
    ): Response<LayoutResponse>
    
    @DELETE("layouts/{id}")
    suspend fun deleteLayout(@Path("id") id: String): Response<DeleteLayoutResponse>
    
    @PUT("layouts/{id}/sections/{sectionId}")
    suspend fun updateSection(
        @Path("id") layoutId: String,
        @Path("sectionId") sectionId: String,
        @Body request: UpdateSectionRequest
    ): Response<LayoutResponse>
    
    // Media (for layout builder)
    @GET("media")
    suspend fun getMedia(): Response<MediaListResponse>
    
    // Schedules
    @GET("schedules")
    suspend fun getSchedules(): Response<ScheduleListResponse>
    
    @POST("schedules")
    suspend fun createSchedule(@Body request: CreateScheduleRequest): Response<ScheduleResponse>
    
    @PUT("schedules/{id}")
    suspend fun updateSchedule(
        @Path("id") id: String,
        @Body request: UpdateScheduleRequest
    ): Response<ScheduleResponse>
    
    @DELETE("schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): Response<Unit>
    
    @GET("schedules/active")
    suspend fun getActiveSchedules(
        @Query("displayId") displayId: String? = null,
        @Query("timezone") timezone: String = "Asia/Kolkata"
    ): Response<ActiveSchedulesResponse>
    
    // Displays (for schedule assignment)
    @GET("displays")
    suspend fun getDisplays(): Response<DisplaysResponse>
}

// Dashboard Summary Response
data class DashboardSummary(
    val role: String,
    
    // Super Admin
    val totalClients: Int? = null,
    val totalDisplays: Int? = null,
    val onlineDisplays: Int? = null,
    
    // Client Admin
    val userAdmins: Int? = null,
    val displayLimit: Int? = null,
    val license: LicenseInfo? = null,
    
    // User Admin
    val displays: DisplayStats? = null,
    val mediaCount: Int? = null,
    val playlistCount: Int? = null,
    val storageBytes: Long? = null
)

data class LicenseInfo(
    val status: String,
    val expiry: String?
)

data class DisplayStats(
    val total: Int,
    val online: Int,
    val offline: Int
)
