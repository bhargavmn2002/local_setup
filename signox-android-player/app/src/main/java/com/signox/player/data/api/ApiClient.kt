package com.signox.player.data.api

import android.net.Uri
import com.signox.player.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // HARDCODED SERVER URL - App will always connect to this URL (canonical: signoxcms.com, no www)
    // No server configuration screen will be shown
    private const val FIXED_BASE_URL = "https://signoxcms.com/api"
    private var baseUrl: String = FIXED_BASE_URL
    private var retrofit: Retrofit? = null
    
    fun setBaseUrl(url: String) {
        // Ignored - URL is permanently fixed to FIXED_BASE_URL
        // This method is kept for compatibility but does nothing
    }
    
    fun getBaseUrl(): String = baseUrl
    
    fun hasBaseUrl(): Boolean = true
    
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val currentBaseUrl = baseUrl
            
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            retrofit = Retrofit.Builder()
                .baseUrl("$currentBaseUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    
    val api: SignoXApi by lazy {
        getRetrofit().create(SignoXApi::class.java)
    }
    
    /**
     * Build a playable URL for this app. Relative paths use the CMS origin (API base without `/api`).
     * Absolute URLs are passed through **unless** they point at our content paths (`/uploads`, `/hls/`, `/downloads`)
     * with a **different host** (e.g. `http://localhost:5000/...` or a LAN IP from dev) — those are rewritten
     * to the current CMS origin so production APKs still work when the DB has stale full URLs.
     */
    fun getMediaUrl(mediaUrl: String): String {
        val mediaOrigin = FIXED_BASE_URL.replace("/api", "").trimEnd('/')
        val trimmed = mediaUrl.trim()
        if (trimmed.isEmpty()) return trimmed

        fun isOurContentPath(path: String?): Boolean {
            if (path.isNullOrBlank()) return false
            return path.startsWith("/uploads") ||
                path.contains("/hls/") ||
                path.startsWith("/downloads")
        }

        val isAbsolute = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)

        if (!isAbsolute) {
            val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            return Uri.parse(mediaOrigin).buildUpon().path(path).build().toString()
        }

        val parsed = Uri.parse(trimmed)
        val path = parsed.path
        if (!isOurContentPath(path)) {
            return trimmed
        }

        return Uri.parse(mediaOrigin).buildUpon()
            .path(path)
            .encodedQuery(parsed.encodedQuery)
            .encodedFragment(parsed.encodedFragment)
            .build()
            .toString()
    }
}