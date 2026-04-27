package com.signox.dashboard.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("role")
    val role: UserRole,
    
    @SerializedName("staffRole")
    val staffRole: StaffRole? = null,
    
    @SerializedName("isActive")
    val isActive: Boolean,
    
    @SerializedName("managedByClientAdminId")
    val managedByClientAdminId: String? = null,
    
    @SerializedName("clientProfile")
    val clientProfile: ClientProfile? = null
) : Parcelable

enum class UserRole {
    @SerializedName("SUPER_ADMIN")
    SUPER_ADMIN,
    
    @SerializedName("CLIENT_ADMIN")
    CLIENT_ADMIN,
    
    @SerializedName("USER_ADMIN")
    USER_ADMIN,
    
    @SerializedName("STAFF")
    STAFF
}

enum class StaffRole {
    @SerializedName("DISPLAY_MANAGER")
    DISPLAY_MANAGER,
    
    @SerializedName("BROADCAST_MANAGER")
    BROADCAST_MANAGER,
    
    @SerializedName("CONTENT_MANAGER")
    CONTENT_MANAGER,
    
    @SerializedName("CMS_VIEWER")
    CMS_VIEWER,
    
    @SerializedName("POP_MANAGER")
    POP_MANAGER
}

@Parcelize
data class ClientProfile(
    @SerializedName("clientId")
    val clientId: String? = null,
    
    @SerializedName("maxDisplays")
    val maxDisplays: Int,
    
    @SerializedName("maxUsers")
    val maxUsers: Int,
    
    @SerializedName("maxStorageMB")
    val maxStorageMB: Int? = null,
    
    @SerializedName("licenseExpiry")
    val licenseExpiry: String? = null,
    
    @SerializedName("companyName")
    val companyName: String? = null,
    
    @SerializedName("contactEmail")
    val contactEmail: String? = null,
    
    @SerializedName("contactPhone")
    val contactPhone: String? = null
) : Parcelable

@Parcelize
data class ClientAdmin(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("isActive")
    val isActive: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("clientProfile")
    val clientProfile: ClientProfile? = null,
    
    @SerializedName("displaysUsed")
    val displaysUsed: Int? = null
) : Parcelable

data class CreateClientAdminRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("companyName")
    val companyName: String,
    
    @SerializedName("maxDisplays")
    val maxDisplays: Int = 10,
    
    @SerializedName("maxUsers")
    val maxUsers: Int = 5,
    
    @SerializedName("maxStorageMB")
    val maxStorageMB: Int = 25,
    
    @SerializedName("licenseExpiry")
    val licenseExpiry: String? = null
)

data class UpdateClientAdminRequest(
    @SerializedName("companyName")
    val companyName: String,
    
    @SerializedName("maxDisplays")
    val maxDisplays: Int,
    
    @SerializedName("maxUsers")
    val maxUsers: Int,
    
    @SerializedName("maxStorageMB")
    val maxStorageMB: Int,
    
    @SerializedName("licenseExpiry")
    val licenseExpiry: String? = null,
    
    @SerializedName("contactEmail")
    val contactEmail: String? = null,
    
    @SerializedName("contactPhone")
    val contactPhone: String? = null
)

data class ClientAdminsResponse(
    @SerializedName("clientAdmins")
    val clientAdmins: List<ClientAdmin>
)
