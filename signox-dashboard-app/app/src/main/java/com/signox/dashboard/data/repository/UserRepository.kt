package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.UserApiService
import com.signox.dashboard.data.api.safeApiCall
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService
) {
    
    suspend fun getUsers(): NetworkResult<List<UserManagement>> = withContext(Dispatchers.IO) {
        val result = safeApiCall { userApiService.getUsers() }
        when (result) {
            is NetworkResult.Success -> {
                val users = result.data?.users ?: emptyList()
                NetworkResult.Success(users)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to load users")
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }
    
    suspend fun createUser(request: CreateUserRequest): NetworkResult<UserManagement> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.createUser(request) }
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.user?.let { NetworkResult.Success(it) }
                        ?: NetworkResult.Error("No user data returned")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to create user")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun updateUser(userId: String, request: UpdateUserRequest): NetworkResult<UserManagement> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.updateUser(userId, request) }
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.user?.let { NetworkResult.Success(it) }
                        ?: NetworkResult.Error("No user data returned")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to update user")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun deleteUser(userId: String): NetworkResult<String> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.deleteUser(userId) }
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data?.message ?: "User deleted successfully")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to delete user")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun bulkDeleteUsers(userIds: List<String>): NetworkResult<String> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.bulkDeleteUsers(userIds) }
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data?.message ?: "Users deleted successfully")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to delete users")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun resetPassword(userId: String, newPassword: String): NetworkResult<String> = 
        withContext(Dispatchers.IO) {
            val request = ResetPasswordRequest(newPassword)
            val result = safeApiCall { userApiService.resetPassword(userId, request) }
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success("Password reset successfully")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to reset password")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun getCurrentUserProfile(): NetworkResult<UserManagement> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.getCurrentUserProfile() }
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.user?.let { NetworkResult.Success(it) }
                        ?: NetworkResult.Error("No profile data returned")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to load profile")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    // Client Admin methods (SUPER_ADMIN only)
    suspend fun getClientAdmins(): NetworkResult<List<ClientAdmin>> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.getClientAdmins() }
            when (result) {
                is NetworkResult.Success -> {
                    val clients = result.data?.clientAdmins ?: emptyList()
                    NetworkResult.Success(clients)
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to load client admins")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun createClientAdmin(request: CreateClientAdminRequest): NetworkResult<UserManagement> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.createClientAdmin(request) }
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.user?.let { NetworkResult.Success(it) }
                        ?: NetworkResult.Error("No user data returned")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to create client admin")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun updateClientAdmin(
        clientAdminId: String, 
        request: UpdateClientAdminRequest
    ): NetworkResult<UserManagement> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.updateClientAdmin(clientAdminId, request) }
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.user?.let { NetworkResult.Success(it) }
                        ?: NetworkResult.Error("No user data returned")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to update client admin")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun toggleClientAdminStatus(clientAdminId: String): NetworkResult<String> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.toggleClientAdminStatus(clientAdminId) }
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success("Status updated successfully")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to update status")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
    
    suspend fun deleteClientAdmin(clientAdminId: String): NetworkResult<String> = 
        withContext(Dispatchers.IO) {
            val result = safeApiCall { userApiService.deleteClientAdmin(clientAdminId) }
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data?.message ?: "Client admin deleted successfully")
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to delete client admin")
                is NetworkResult.Loading -> NetworkResult.Loading()
            }
        }
}
