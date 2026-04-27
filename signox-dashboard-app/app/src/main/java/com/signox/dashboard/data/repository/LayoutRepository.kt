package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayoutRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    suspend fun getLayouts(): Result<List<Layout>> {
        return try {
            val response = apiService.getLayouts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.layouts)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch layouts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLayout(id: String): Result<Layout> {
        return try {
            val response = apiService.getLayout(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.layout)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch layout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createLayout(request: CreateLayoutRequest): Result<Layout> {
        return try {
            val response = apiService.createLayout(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.layout)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create layout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateLayout(id: String, request: UpdateLayoutRequest): Result<Layout> {
        return try {
            val response = apiService.updateLayout(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.layout)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update layout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteLayout(id: String): Result<String> {
        return try {
            val response = apiService.deleteLayout(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete layout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSection(
        layoutId: String,
        sectionId: String,
        request: UpdateSectionRequest
    ): Result<Layout> {
        return try {
            val response = apiService.updateSection(layoutId, sectionId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.layout)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update section"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMedia(): Result<List<Media>> {
        return try {
            val response = apiService.getMedia()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.mediaList)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
