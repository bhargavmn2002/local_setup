package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.api.SettingsApiService
import com.signox.dashboard.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val apiService: SettingsApiService
) {
    
    suspend fun getProfile(): NetworkResult<UserProfile> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(response.message() ?: "Failed to load profile")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun getHierarchy(): NetworkResult<HierarchyInfo> {
        return try {
            val response = apiService.getProfileWithHierarchy()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!.hierarchy)
            } else {
                NetworkResult.Error(response.message() ?: "Failed to load hierarchy")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfile> {
        return try {
            val response = apiService.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(response.message() ?: "Failed to update profile")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun changePassword(request: ChangePasswordRequest): NetworkResult<String> {
        return try {
            val response = apiService.changePassword(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!.message)
            } else {
                NetworkResult.Error(response.message() ?: "Failed to change password")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun getAccountInfo(): NetworkResult<AccountInfo> {
        return try {
            val response = apiService.getAccountInfo()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(response.message() ?: "Failed to load account info")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
}
