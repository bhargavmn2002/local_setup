package com.signox.dashboard.data.api

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : NetworkResult<T>(data)
    class Error<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)
    class Loading<T> : NetworkResult<T>()
}

// Extension function to handle API responses
suspend fun <T> safeApiCall(
    apiCall: suspend () -> retrofit2.Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let {
                NetworkResult.Success(it)
            } ?: NetworkResult.Error("Empty response body")
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Unauthorized. Please login again."
                403 -> "Access denied"
                404 -> "Resource not found"
                500 -> "Server error. Please try again later."
                else -> response.message() ?: "Unknown error occurred"
            }
            NetworkResult.Error(errorMessage)
        }
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Network error occurred")
    }
}
