package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    
    @GET("users")
    suspend fun getUsers(): Response<UsersListResponse>
    
    @POST("users")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<UserResponse>
    
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
    
    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Path("id") userId: String
    ): Response<DeleteUserResponse>
    
    @POST("users/bulk-delete")
    suspend fun bulkDeleteUsers(
        @Body userIds: List<String>
    ): Response<DeleteUserResponse>
    
    @PUT("users/{id}/reset-password")
    suspend fun resetPassword(
        @Path("id") userId: String,
        @Body request: ResetPasswordRequest
    ): Response<UserResponse>
    
    @GET("users/profile")
    suspend fun getCurrentUserProfile(): Response<UserResponse>
    
    // Client Admin endpoints (SUPER_ADMIN only)
    @GET("users/client-admins")
    suspend fun getClientAdmins(): Response<ClientAdminsResponse>
    
    @POST("users/client-admin")
    suspend fun createClientAdmin(
        @Body request: CreateClientAdminRequest
    ): Response<UserResponse>
    
    @PUT("users/client-admins/{id}")
    suspend fun updateClientAdmin(
        @Path("id") clientAdminId: String,
        @Body request: UpdateClientAdminRequest
    ): Response<UserResponse>
    
    @PATCH("users/client-admins/{id}/status")
    suspend fun toggleClientAdminStatus(
        @Path("id") clientAdminId: String
    ): Response<UserResponse>
    
    @DELETE("users/client-admins/{id}")
    suspend fun deleteClientAdmin(
        @Path("id") clientAdminId: String
    ): Response<DeleteUserResponse>
}
