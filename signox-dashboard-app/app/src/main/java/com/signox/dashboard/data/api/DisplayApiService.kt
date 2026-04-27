package com.signox.dashboard.data.api

import com.signox.dashboard.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface DisplayApiService {
    
    @GET("displays")
    suspend fun getDisplays(): Response<DisplaysResponse>
    
    @GET("displays/{id}")
    suspend fun getDisplay(@Path("id") id: String): Response<Display>
    
    @POST("displays/pairing-code")
    suspend fun generatePairingCode(): Response<PairingCodeResponse>
    
    @POST("displays/pair")
    suspend fun pairDisplay(@Body request: PairDisplayRequest): Response<PairDisplayResponse>
    
    @PATCH("displays/{id}")
    suspend fun updateDisplay(
        @Path("id") id: String,
        @Body request: UpdateDisplayRequest
    ): Response<Display>
    
    @DELETE("displays/{id}")
    suspend fun deleteDisplay(@Path("id") id: String): Response<DeleteDisplayResponse>
    
    @POST("displays/check-status")
    suspend fun checkPairingStatus(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @GET("displays/{id}/status")
    suspend fun getDisplayStatus(@Path("id") id: String): Response<Map<String, Any>>
}
