package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface SettingsApiService {
    
    // Profile endpoints
    @GET("users/profile/settings")
    suspend fun getProfile(): Response<UserProfile>
    
    @GET("users/profile")
    suspend fun getProfileWithHierarchy(): Response<ProfileHierarchyResponse>
    
    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfile>
    
    @PUT("users/profile/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>
    
    // Account info (for CLIENT_ADMIN)
    @GET("users/me/account")
    suspend fun getAccountInfo(): Response<AccountInfo>
}

data class MessageResponse(
    val message: String
)

data class ProfileHierarchyResponse(
    val user: UserProfileData,
    val hierarchy: HierarchyInfo
)

data class UserProfileData(
    val id: String,
    val name: String?,
    val email: String,
    val role: String,
    val staffRole: String?,
    val isActive: Boolean,
    val managedByClientAdminId: String?,
    val clientProfile: ClientProfileData?
)

data class ClientProfileData(
    val companyName: String
)
