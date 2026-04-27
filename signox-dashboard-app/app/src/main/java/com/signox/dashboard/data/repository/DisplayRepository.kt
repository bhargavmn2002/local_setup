package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.DisplayApiService
import com.signox.dashboard.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayRepository @Inject constructor(
    private val apiService: DisplayApiService
) {
    
    suspend fun getDisplays(): Result<List<Display>> {
        return try {
            val response = apiService.getDisplays()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.displays)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch displays"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDisplay(id: String): Result<Display> {
        return try {
            val response = apiService.getDisplay(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch display"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generatePairingCode(): Result<PairingCodeResponse> {
        return try {
            val response = apiService.generatePairingCode()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to generate pairing code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun pairDisplay(
        pairingCode: String,
        name: String,
        managedByUserId: String? = null
    ): Result<PairDisplayResponse> {
        return try {
            val request = PairDisplayRequest(pairingCode, name, managedByUserId)
            val response = apiService.pairDisplay(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null && errorBody.contains("error")) {
                    // Try to parse error message from JSON
                    try {
                        val jsonError = org.json.JSONObject(errorBody)
                        jsonError.getString("error")
                    } catch (e: Exception) {
                        response.message() ?: "Failed to pair display"
                    }
                } else {
                    response.message() ?: "Failed to pair display"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDisplay(
        id: String,
        name: String? = null,
        playlistId: String? = null,
        layoutId: String? = null,
        location: String? = null,
        orientation: String? = null,
        tags: List<String>? = null
    ): Result<Display> {
        return try {
            val request = UpdateDisplayRequest(name, playlistId, layoutId, location, orientation, tags)
            val response = apiService.updateDisplay(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update display"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteDisplay(id: String): Result<String> {
        return try {
            val response = apiService.deleteDisplay(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete display"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
