package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.api.safeApiCall
import com.signox.dashboard.data.local.TokenManager
import com.signox.dashboard.data.model.LoginRequest
import com.signox.dashboard.data.model.LoginResponse
import com.signox.dashboard.data.model.ProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    
    suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        val result = safeApiCall {
            apiService.login(LoginRequest(email, password))
        }
        
        // Save token and user info if login successful
        if (result is NetworkResult.Success) {
            result.data?.let { response ->
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUserInfo(
                    userId = response.user.id,
                    email = response.user.email,
                    role = response.user.role.name
                )
            }
        }
        
        return result
    }
    
    suspend fun logout(): NetworkResult<Unit> {
        val result = safeApiCall {
            apiService.logout()
        }
        
        // Clear local data regardless of API response
        tokenManager.clearAll()
        
        return result
    }
    
    suspend fun getMe(): NetworkResult<ProfileResponse> {
        return safeApiCall {
            apiService.getMe()
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    suspend fun clearSession() {
        tokenManager.clearAll()
    }
}
