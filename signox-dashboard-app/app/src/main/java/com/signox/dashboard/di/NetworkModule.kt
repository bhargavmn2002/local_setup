package com.signox.dashboard.di

import com.signox.dashboard.BuildConfig
import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.api.AuthInterceptor
import com.signox.dashboard.data.api.DisplayApiService
import com.signox.dashboard.data.api.MediaApiService
import com.signox.dashboard.data.local.ServerConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        // Create a trust manager that trusts all certificates (for development only)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Trust all hostnames
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                
                // Log response for debugging
                if (!response.isSuccessful) {
                    val responseBody = response.peekBody(1024)
                    android.util.Log.e("OkHttp", "Error ${response.code} for ${request.url}: ${responseBody.string()}")
                }
                
                response
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        serverConfigManager: ServerConfigManager
    ): Retrofit {
        // Get the server URL from config, fallback to default
        val baseUrl = runBlocking {
            serverConfigManager.getServerUrlSync()
        }
        
        // Create Gson with lenient parsing
        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
        
        return Retrofit.Builder()
            .baseUrl("$baseUrl/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDisplayApiService(retrofit: Retrofit): DisplayApiService {
        return retrofit.create(DisplayApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideMediaApiService(retrofit: Retrofit): MediaApiService {
        return retrofit.create(MediaApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsApiService(retrofit: Retrofit): com.signox.dashboard.data.api.AnalyticsApiService {
        return retrofit.create(com.signox.dashboard.data.api.AnalyticsApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): com.signox.dashboard.data.api.UserApiService {
        return retrofit.create(com.signox.dashboard.data.api.UserApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideSettingsApiService(retrofit: Retrofit): com.signox.dashboard.data.api.SettingsApiService {
        return retrofit.create(com.signox.dashboard.data.api.SettingsApiService::class.java)
    }
}
