package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("user")
    val user: User
)

data class ProfileResponse(
    @SerializedName("user")
    val user: User,
    
    @SerializedName("hierarchy")
    val hierarchy: Hierarchy? = null
)

data class Hierarchy(
    @SerializedName("clientAdmin")
    val clientAdmin: AdminInfo? = null,
    
    @SerializedName("userAdmin")
    val userAdmin: AdminInfo? = null,
    
    @SerializedName("companyName")
    val companyName: String? = null
)

data class AdminInfo(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("name")
    val name: String? = null
)
