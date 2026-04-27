package com.signox.dashboard.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.local.ServerConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@HiltViewModel
class ServerConfigViewModel @Inject constructor(
    private val serverConfigManager: ServerConfigManager
) : ViewModel() {
    
    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val httpClient: OkHttpClient by lazy {
        // Create a trust manager that trusts all certificates (for development only)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    
    fun loadServerUrl() {
        viewModelScope.launch {
            serverConfigManager.getServerUrl().collect { url ->
                _serverUrl.value = url
            }
        }
    }
    
    fun testAndSaveServerUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            _saveSuccess.value = false
            
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        // Test connection to the server
                        val testUrl = "${url.removeSuffix("/")}/health"
                        val request = Request.Builder()
                            .url(testUrl)
                            .get()
                            .build()
                        
                        val response = httpClient.newCall(request).execute()
                        val isSuccessful = response.isSuccessful
                        val code = response.code
                        response.close()
                        
                        Pair(isSuccessful, code)
                    } catch (e: Exception) {
                        throw e
                    }
                }
                
                if (result.first) {
                    // Connection successful, save the URL
                    serverConfigManager.saveServerUrl(url)
                    _saveSuccess.value = true
                } else {
                    _error.value = "Server responded with error: ${result.second}. Please check the URL."
                }
                
            } catch (e: java.net.UnknownHostException) {
                _error.value = "Cannot reach server. Please check the IP address."
            } catch (e: java.net.ConnectException) {
                _error.value = "Connection refused. Make sure the server is running on port ${extractPort(url)}."
            } catch (e: java.net.SocketTimeoutException) {
                _error.value = "Connection timeout. Server is not responding."
            } catch (e: IllegalArgumentException) {
                _error.value = "Invalid URL format. Please check the address."
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun extractPort(url: String): String {
        return try {
            val portMatch = Regex(":(\\d+)").find(url)
            portMatch?.groupValues?.get(1) ?: "5000"
        } catch (e: Exception) {
            "5000"
        }
    }
}
