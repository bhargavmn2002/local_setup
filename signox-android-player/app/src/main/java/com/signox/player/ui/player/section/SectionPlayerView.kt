package com.signox.player.ui.player.section

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.signox.player.BuildConfig
import com.signox.player.R
import com.signox.player.data.dto.LayoutItemDto
import com.signox.player.data.dto.LayoutSectionDto
import com.signox.player.data.dto.MediaType
import com.signox.player.data.dto.isScrollTextSection
import com.signox.player.databinding.ViewSectionPlayerBinding
import com.signox.player.media.PlaybackDataSource

class SectionPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewSectionPlayerBinding
    private var section: LayoutSectionDto? = null
    private var currentItemIndex = 0
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var advanceRunnable: Runnable? = null
    private var textScrollAnimator: ValueAnimator? = null
    
    companion object {
        private const val TAG = "SectionPlayerView"
        private const val DEFAULT_IMAGE_DURATION = 10 // seconds
    }
    
    init {
        binding = ViewSectionPlayerBinding.inflate(LayoutInflater.from(context), this, true)
        initializePlayer()
    }
    
    fun setSection(section: LayoutSectionDto) {
        this.section = section
        currentItemIndex = 0
    }
    
    private fun initializePlayer() {
        val mediaSourceFactory = DefaultMediaSourceFactory(
            PlaybackDataSource.defaultDataSourceFactory(context)
        )
        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            // Always advance to next item when video ends (unless single item with loopEnabled)
                            if (hasMultipleItems() || !isLoopEnabled()) {
                                advanceToNext()
                            }
                        }
                        Player.STATE_READY -> {
                            Log.d(TAG, "Video ready in section ${section?.id}")
                        }
                    }
                }
                
                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    Log.e(TAG, "ExoPlayer error in section ${section?.id}: ${error.message} code=${error.errorCode}")
                    error.cause?.let { Log.e(TAG, "Cause: ${it.message}", it) }
                    advanceToNext() // Skip to next on error
                }
            })
            
            // Enable audio for videos (set volume to 100%)
            volume = 1f
        }
        
        binding.playerView.player = exoPlayer
        binding.playerView.useController = false // Hide controls for kiosk mode
    }
    
    fun startPlayback() {
        val section = this.section
        if (section == null) {
            Log.w(TAG, "No section")
            return
        }

        if (section.isScrollTextSection() && section.textConfig != null) {
            startScrollingText()
            return
        }

        if (section.items.isEmpty()) {
            Log.w(TAG, "No section or empty items")
            return
        }

        clearTextMode()
        currentItemIndex = 0
        playCurrentItem()
    }

    private fun clearTextMode() {
        textScrollAnimator?.cancel()
        textScrollAnimator = null
        binding.scrollingTextView.visibility = GONE
        binding.scrollingTextView.translationX = 0f
        binding.scrollingTextView.translationY = 0f
        binding.root.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun parseColorSafe(hex: String?): Int {
        if (hex.isNullOrBlank() || hex.equals("transparent", ignoreCase = true)) {
            return Color.TRANSPARENT
        }
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            Color.BLACK
        }
    }

    private fun typefaceForWeight(weight: String?): Typeface {
        return when (weight?.lowercase()) {
            "bold", "bolder" -> Typeface.DEFAULT_BOLD
            "lighter" -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
    }

    private fun startScrollingText() {
        val section = this.section ?: return
        val config = section.textConfig ?: return

        cancelAdvance()
        exoPlayer?.stop()
        clearTextMode()

        binding.playerView.visibility = GONE
        binding.imageView.visibility = GONE
        binding.scrollingTextView.visibility = VISIBLE

        val tv = binding.scrollingTextView
        val dir = config.direction.lowercase().trim()
        val horizontal = dir == "left-to-right" || dir == "right-to-left"
        // Match web ScrollingText: vertical modes use one char per line ("train" column)
        val displayText =
            if (horizontal) config.text
            else config.text.toCharArray().joinToString("\n")
        tv.text = displayText
        // CMS field is "Font Size (px)" — use raw px so size/speed match web signage; SP scales with system text size and skews marquee timing vs px/sec.
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, config.fontSize.toFloat())
        tv.setTextColor(parseColorSafe(config.textColor))
        tv.typeface = typefaceForWeight(config.fontWeight)
        tv.gravity = Gravity.CENTER_VERTICAL

        tv.isSingleLine = horizontal
        if (!horizontal) {
            tv.maxLines = Int.MAX_VALUE
            tv.setHorizontallyScrolling(false)
        } else {
            tv.setHorizontallyScrolling(true)
        }

        binding.root.setBackgroundColor(parseColorSafe(config.backgroundColor))

        tv.post {
            val containerW = width
            val containerH = height
            if (containerW <= 0 || containerH <= 0) {
                Log.w(TAG, "Section ${section.id} — container not laid out for scroll text")
                return@post
            }

            val wSpec = if (horizontal) {
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            } else {
                View.MeasureSpec.makeMeasureSpec(containerW, View.MeasureSpec.EXACTLY)
            }
            val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            tv.measure(wSpec, hSpec)
            val tw = tv.measuredWidth.toFloat().coerceAtLeast(1f)
            val th = tv.measuredHeight.toFloat().coerceAtLeast(1f)
            val speedPxPerSec =
                (config.speed ?: 50.0).coerceAtLeast(5.0).coerceAtMost(2000.0).toFloat()

            val distancePx = when (dir) {
                "top-to-bottom", "bottom-to-top" -> containerH.toFloat() + th
                else -> containerW.toFloat() + tw
            }
            // Web: durationMs = distance / pxPerSec * 1000 (use double so 100 vs 50 isn’t quantized away)
            val durationMs =
                ((distancePx.toDouble() / speedPxPerSec.toDouble()) * 1000.0)
                    .toLong()
                    .coerceIn(32L, 600_000L)

            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "Scroll text section=${section.id} dir=$dir speedPxPerSec=$speedPxPerSec " +
                        "dist=${distancePx}px durMs=$durationMs (raw speed from config=${config.speed})"
                )
            }

            textScrollAnimator?.cancel()
            val cw = containerW.toFloat()
            val ch = containerH.toFloat()
            textScrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { va ->
                    val u = va.animatedFraction
                    when (dir) {
                        "right-to-left" -> tv.translationX = cw + (-tw - cw) * u
                        "left-to-right" -> tv.translationX = -tw + (cw + tw) * u
                        "top-to-bottom" -> tv.translationY = -th + (ch + th) * u
                        "bottom-to-top" -> tv.translationY = ch + (-th - ch) * u
                        else -> tv.translationX = -tw + (cw + tw) * u
                    }
                }
                start()
            }
        }
    }
    
    private fun playCurrentItem() {
        val currentItem = getCurrentItem() ?: return
        val sectionId = section?.id ?: "unknown"
        
        Log.d(TAG, "Playing item ${currentItemIndex + 1}/${section?.items?.size} in section $sectionId")
        
        // Update screen orientation if item has specific orientation
        updateScreenOrientation(currentItem.orientation)
        
        when (currentItem.media.type) {
            MediaType.IMAGE -> playImage(currentItem)
            MediaType.VIDEO -> playVideo(currentItem)
        }
    }
    
    private fun updateScreenOrientation(orientation: String?) {
        orientation?.let {
            (context as? android.app.Activity)?.requestedOrientation = when (it.uppercase()) {
                "PORTRAIT" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "LANDSCAPE" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> return
            }
            Log.d(TAG, "Updated screen orientation to: $orientation")
        }
    }
    
    private fun playImage(item: LayoutItemDto) {
        clearTextMode()
        // Hide video player, show image view
        binding.playerView.visibility = GONE
        binding.imageView.visibility = VISIBLE
        
        // Apply rotation
        binding.imageView.rotation = (item.rotation ?: 0).toFloat()
        
        // Apply scale type based on resizeMode
        binding.imageView.scaleType = when (item.resizeMode?.uppercase()) {
            "FILL" -> android.widget.ImageView.ScaleType.CENTER_CROP
            "STRETCH" -> android.widget.ImageView.ScaleType.FIT_XY
            "FIT" -> android.widget.ImageView.ScaleType.FIT_CENTER
            else -> android.widget.ImageView.ScaleType.FIT_CENTER
        }
        
        // Use OfflineMediaLoader for offline support
        val offlineLoader = com.signox.player.cache.OfflineMediaLoader.getInstance(context)
        val imageUrl = offlineLoader.loadMedia(item.media.url)
        
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_error_placeholder)
            .into(binding.imageView)
        
        // Schedule advance after duration - use item duration, media duration, or default
        val duration = item.duration ?: item.media.duration ?: DEFAULT_IMAGE_DURATION
        scheduleAdvance(duration * 1000L)
    }
    
    private fun playVideo(item: LayoutItemDto) {
        clearTextMode()
        // Hide image view, show video player
        binding.imageView.visibility = GONE
        binding.playerView.visibility = VISIBLE
        
        // Apply rotation to video player
        binding.playerView.rotation = (item.rotation ?: 0).toFloat()
        
        // Apply resize mode to video player
        binding.playerView.resizeMode = when (item.resizeMode?.uppercase()) {
            "FILL" -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            "STRETCH" -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            "FIT" -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            else -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        
        // Prefer MP4 originalUrl when present (same as playlist player); rewrite host via OfflineMediaLoader/getMediaUrl
        val isHlsMaster =
            item.media.url.contains("/hls/") && item.media.url.endsWith("/index.m3u8")
        val hasOriginalUrl = !item.media.originalUrl.isNullOrEmpty()
        val videoUrlToUse = when {
            hasOriginalUrl -> item.media.originalUrl!!
            else -> item.media.url
        }
        if (isHlsMaster && hasOriginalUrl) {
            Log.d(TAG, "Using originalUrl (MP4) for layout section video instead of HLS master")
        }

        val offlineLoader = com.signox.player.cache.OfflineMediaLoader.getInstance(context)
        val videoUrl = offlineLoader.loadMedia(videoUrlToUse)
        Log.d(TAG, "Layout video: selected=$videoUrlToUse playback=$videoUrl")
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            
            // Loop if single item and section loopEnabled
            repeatMode = if (!hasMultipleItems() && isLoopEnabled()) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
            
            prepare()
            play()
        }
        
        // If has duration, schedule advance (for looping videos or timed videos)
        if (item.duration != null) {
            scheduleAdvance(item.duration * 1000L)
        }
        // If no duration and multiple items, advance will happen in onPlaybackStateChanged when STATE_ENDED
    }
    
    private fun scheduleAdvance(delayMs: Long) {
        cancelAdvance()
        advanceRunnable = Runnable {
            advanceToNext()
        }
        handler.postDelayed(advanceRunnable!!, delayMs)
    }
    
    private fun cancelAdvance() {
        advanceRunnable?.let { handler.removeCallbacks(it) }
        advanceRunnable = null
    }
    
    private fun advanceToNext() {
        val section = this.section ?: return
        
        if (section.items.size <= 1) {
            // Single item, restart it if looping enabled
            if (isLoopEnabled()) {
                currentItemIndex = 0
            }
        } else {
            // Multiple items, go to next
            currentItemIndex = (currentItemIndex + 1) % section.items.size
        }
        
        Log.d(TAG, "Advancing to item ${currentItemIndex + 1}/${section.items.size} in section ${section.id}")
        
        // Stop current playback
        exoPlayer?.stop()
        cancelAdvance()
        
        // Play next item
        playCurrentItem()
    }
    
    private fun getCurrentItem(): LayoutItemDto? {
        return section?.items?.getOrNull(currentItemIndex)
    }
    
    private fun hasMultipleItems(): Boolean {
        return (section?.items?.size ?: 0) > 1
    }
    
    private fun isLoopEnabled(): Boolean {
        return section?.loopEnabled ?: false
    }
    
    fun pause() {
        exoPlayer?.pause()
        cancelAdvance()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            textScrollAnimator?.pause()
        } else {
            textScrollAnimator?.cancel()
        }
    }

    fun resume() {
        exoPlayer?.play()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            textScrollAnimator?.resume()
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        cancelAdvance()
        textScrollAnimator?.cancel()
        textScrollAnimator = null
    }
}