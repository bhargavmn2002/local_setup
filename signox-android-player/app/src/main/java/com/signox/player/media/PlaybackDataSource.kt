package com.signox.player.media

import android.content.Context
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

/**
 * Shared HTTP settings for ExoPlayer. Some origins return 403 or empty responses for
 * ExoPlayer's default user agent; a normal browser UA avoids that.
 */
object PlaybackDataSource {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun httpFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT)

    fun defaultDataSourceFactory(context: Context): DefaultDataSource.Factory =
        DefaultDataSource.Factory(context, httpFactory())
}
