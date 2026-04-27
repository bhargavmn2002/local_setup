package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.AnalyticsSummary
import com.signox.dashboard.data.model.PlaybackLog
import com.signox.dashboard.data.model.Report
import retrofit2.Response
import retrofit2.http.*

interface AnalyticsApiService {
    
    @GET("analytics/summary")
    suspend fun getAnalyticsSummary(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<AnalyticsSummary>
    
    @GET("analytics/proof-of-play")
    suspend fun getProofOfPlay(
        @Query("displayId") displayId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PlaybackLogsResponse>
    
    @GET("analytics/reports")
    suspend fun getReports(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ReportsListResponse>
    
    @POST("analytics/reports")
    suspend fun generateReport(
        @Body reportRequest: ReportRequest
    ): Response<Report>
    
    @GET("analytics/reports/{id}")
    suspend fun getReportDetails(
        @Path("id") reportId: String
    ): Response<Report>
    
    @DELETE("analytics/reports/{id}")
    suspend fun deleteReport(
        @Path("id") reportId: String
    ): Response<DeleteReportResponse>
}

data class ReportRequest(
    val name: String,
    val type: String,
    val startDate: String,
    val endDate: String,
    val displays: List<String>? = null
)

data class PlaybackLogsResponse(
    val logs: List<PlaybackLog>
)

data class ReportsListResponse(
    val reports: List<Report>
)

data class DeleteReportResponse(
    val message: String
)
