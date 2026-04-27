package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class AnalyticsSummary(
    @SerializedName("totalDisplays")
    val totalDisplays: Int = 0,
    
    @SerializedName("onlineDisplays")
    val onlineDisplays: Int = 0,
    
    @SerializedName("offlineDisplays")
    val offlineDisplays: Int = 0,
    
    @SerializedName("totalContent")
    val totalContent: Int = 0,
    
    @SerializedName("totalPlaylists")
    val totalPlaylists: Int = 0,
    
    @SerializedName("averageUptime")
    val averageUptime: Double = 0.0,
    
    @SerializedName("totalPlaybackTime")
    val totalPlaybackTime: Long = 0,
    
    @SerializedName("mostPlayedContent")
    val mostPlayedContent: List<ContentStats>? = null,
    
    @SerializedName("displayActivity")
    val displayActivity: List<DisplayActivity>? = null
)

data class ContentStats(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("playCount")
    val playCount: Int = 0,
    
    @SerializedName("totalDuration")
    val totalDuration: Long = 0,
    
    @SerializedName("thumbnail")
    val thumbnail: String? = null
)

data class DisplayActivity(
    @SerializedName("displayId")
    val displayId: String,
    
    @SerializedName("displayName")
    val displayName: String,
    
    @SerializedName("uptime")
    val uptime: Double = 0.0,
    
    @SerializedName("lastSeen")
    val lastSeen: String? = null,
    
    @SerializedName("status")
    val status: String = "offline",
    
    @SerializedName("playbackCount")
    val playbackCount: Int = 0
)

data class PlaybackLog(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("displayId")
    val displayId: String,
    
    @SerializedName("displayName")
    val displayName: String? = null,
    
    @SerializedName("contentId")
    val contentId: String,
    
    @SerializedName("contentName")
    val contentName: String,
    
    @SerializedName("contentType")
    val contentType: String,
    
    @SerializedName("playedAt")
    val playedAt: String,
    
    @SerializedName("duration")
    val duration: Long = 0,
    
    @SerializedName("completed")
    val completed: Boolean = false,
    
    @SerializedName("screenshot")
    val screenshot: String? = null,
    
    @SerializedName("verified")
    val verified: Boolean = false
)

data class Report(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("dateRange")
    val dateRange: DateRange,
    
    @SerializedName("displays")
    val displays: List<String>? = null,
    
    @SerializedName("generatedAt")
    val generatedAt: String,
    
    @SerializedName("status")
    val status: String = "pending",
    
    @SerializedName("fileUrl")
    val fileUrl: String? = null,
    
    @SerializedName("summary")
    val summary: ReportSummary? = null
)

data class DateRange(
    @SerializedName("startDate")
    val startDate: String,
    
    @SerializedName("endDate")
    val endDate: String
)

data class ReportSummary(
    @SerializedName("totalPlaybacks")
    val totalPlaybacks: Int = 0,
    
    @SerializedName("totalDuration")
    val totalDuration: Long = 0,
    
    @SerializedName("averageUptime")
    val averageUptime: Double = 0.0,
    
    @SerializedName("displaysIncluded")
    val displaysIncluded: Int = 0
)
