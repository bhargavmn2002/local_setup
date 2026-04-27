package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class UserManagement(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: UserRole,
    @SerializedName("staffRole") val staffRole: StaffRole? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("managedByClientAdminId") val managedByClientAdminId: String? = null,
    @SerializedName("createdByUserAdminId") val createdByUserAdminId: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("clientProfile") val clientProfile: ClientProfile? = null,
    @SerializedName("displayCount") val displayCount: Int? = null,
    @SerializedName("lastLogin") val lastLogin: String? = null
)

// Request models
data class CreateUserRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("companyName") val companyName: String? = null,
    @SerializedName("maxDisplays") val maxDisplays: Int? = null,
    @SerializedName("maxUsers") val maxUsers: Int? = null,
    @SerializedName("licenseExpiry") val licenseExpiry: String? = null,
    @SerializedName("staffRole") val staffRole: String? = null
)

data class UpdateUserRequest(
    @SerializedName("email") val email: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("staffRole") val staffRole: String? = null
)

data class ResetPasswordRequest(
    @SerializedName("newPassword") val newPassword: String
)

// Response models
data class UsersListResponse(
    @SerializedName("users") val users: List<UserManagement>,
    @SerializedName("total") val total: Int? = null
)

data class UserResponse(
    @SerializedName("user") val user: UserManagement,
    @SerializedName("message") val message: String? = null
)

data class DeleteUserResponse(
    @SerializedName("message") val message: String
)
