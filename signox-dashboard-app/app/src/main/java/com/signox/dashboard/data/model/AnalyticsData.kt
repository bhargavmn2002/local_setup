package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class AnalyticsData(
    @SerializedName("userAdmins") val userAdmins: Int? = null,
    @SerializedName("totalDisplays") val totalDisplays: Int? = null,
    @SerializedName("displayLimit") val displayLimit: Int? = null,
    @SerializedName("license") val license: LicenseInfo? = null,
    @SerializedName("role") val role: String? = null
)

data class LicenseInfo(
    @SerializedName("status") val status: String? = null,
    @SerializedName("expiry") val expiry: String? = null
)

data class AnalyticsResponse(
    @SerializedName("data") val data: AnalyticsData? = null,
    @SerializedName("userAdmins") val userAdmins: Int? = null,
    @SerializedName("totalDisplays") val totalDisplays: Int? = null,
    @SerializedName("displayLimit") val displayLimit: Int? = null,
    @SerializedName("license") val license: LicenseInfo? = null,
    @SerializedName("role") val role: String? = null
)
