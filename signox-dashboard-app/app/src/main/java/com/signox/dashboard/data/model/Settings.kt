package com.signox.dashboard.data.model

import java.util.Date

data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String,
    val role: String,
    val staffRole: String? = null,
    val companyName: String? = null,
    val clientAdminName: String? = null,
    val clientAdminEmail: String? = null,
    val userAdminName: String? = null,
    val userAdminEmail: String? = null,
    val profilePicture: String? = null,
    val createdAt: Date
)

data class HierarchyInfo(
    val companyName: String?,
    val clientAdmin: HierarchyUser?,
    val userAdmin: HierarchyUser?
)

data class HierarchyUser(
    val id: String,
    val email: String,
    val name: String?
)

data class UpdateProfileRequest(
    val name: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val theme: Theme = Theme.AUTO,
    val language: String = "en",
    val autoRefreshInterval: Int = 30 // seconds
)

enum class Theme {
    LIGHT, DARK, AUTO
}

data class AccountInfo(
    val companyName: String?,
    val maxDisplays: Int?,
    val maxUsers: Int?,
    val maxStorageMB: Int?,
    val currentDisplays: Int = 0,
    val currentUsers: Int = 0,
    val currentStorageMB: Int = 0,
    val licenseExpiry: String?,
    val subscriptionStatus: String = "Active"
)
