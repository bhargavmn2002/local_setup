package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.AnalyticsApiService
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.api.ReportRequest
import com.signox.dashboard.data.api.safeApiCall
import com.signox.dashboard.data.model.AnalyticsData
import com.signox.dashboard.data.model.AnalyticsResponse
import com.signox.dashboard.data.model.AnalyticsSummary
import com.signox.dashboard.data.model.PlaybackLog
import com.signox.dashboard.data.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsApiService: AnalyticsApiService
) {
    
    suspend fun getAnalytics(): NetworkResult<AnalyticsData> = withContext(Dispatchers.IO) {
        val result = safeApiCall { analyticsApiService.getAnalyticsSummary() }
        when (result) {
            is NetworkResult.Success -> {
                val summary = result.data
                val analyticsData = AnalyticsData(
                    userAdmins = null, // Not available in summary
                    totalDisplays = summary?.totalDisplays,
                    displayLimit = null, // Not available in summary
                    license = null, // Not available in summary
                    role = null
                )
                NetworkResult.Success(analyticsData)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Failed to load analytics")
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }
    
    suspend fun getAnalyticsSummary(
        startDate: String? = null,
        endDate: String? = null
    ): Result<AnalyticsSummary> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApiService.getAnalyticsSummary(startDate, endDate)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch analytics: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProofOfPlay(
        displayId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1
    ): Result<List<PlaybackLog>> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApiService.getProofOfPlay(displayId, startDate, endDate, page)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.logs)
            } else {
                Result.failure(Exception("Failed to fetch proof of play: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReports(page: Int = 1): Result<List<Report>> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApiService.getReports(page)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.reports)
            } else {
                Result.failure(Exception("Failed to fetch reports: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateReport(
        name: String,
        type: String,
        startDate: String,
        endDate: String,
        displays: List<String>? = null
    ): Result<Report> = withContext(Dispatchers.IO) {
        try {
            val request = ReportRequest(name, type, startDate, endDate, displays)
            val response = analyticsApiService.generateReport(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to generate report: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReportDetails(reportId: String): Result<Report> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApiService.getReportDetails(reportId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch report details: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteReport(reportId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApiService.deleteReport(reportId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete report: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
