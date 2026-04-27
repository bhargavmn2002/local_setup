package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.api.DashboardSummary
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.api.safeApiCall
import com.signox.dashboard.data.model.ProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    suspend fun getUserProfile(): NetworkResult<ProfileResponse> {
        return safeApiCall {
            apiService.getUserProfile()
        }
    }
    
    suspend fun getDashboardSummary(): NetworkResult<DashboardSummary> {
        return safeApiCall {
            apiService.getAnalyticsSummary()
        }
    }
}
