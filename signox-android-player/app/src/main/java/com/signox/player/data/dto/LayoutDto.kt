package com.signox.player.data.dto

import android.os.Parcelable
import com.google.gson.annotations.JsonAdapter
import kotlinx.parcelize.Parcelize

@Parcelize
data class LayoutDto(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val orientation: String? = "LANDSCAPE",
    val sections: List<LayoutSectionDto>
) : Parcelable

/** Matches CMS / web player `textConfig` + API `type: "text"` for SCROLL_TEXT sections. */
@JsonAdapter(ScrollTextConfigDtoDeserializer::class)
@Parcelize
data class ScrollTextConfigDto(
    val text: String = "",
    val direction: String = "left-to-right",
    /** Pixels per second; nullable so Gson leaves null when key missing (avoid 0f = stuck min speed). */
    val speed: Double? = null,
    val fontSize: Int = 24,
    val fontWeight: String = "normal",
    val textColor: String = "#000000",
    val backgroundColor: String = "transparent"
) : Parcelable

@Parcelize
data class LayoutSectionDto(
    val id: String,
    val name: String,
    val order: Int,
    val x: Float, // percentage 0-100
    val y: Float, // percentage 0-100
    val width: Float, // percentage 0-100
    val height: Float, // percentage 0-100
    val loopEnabled: Boolean,
    val frequency: Int? = null,
    /** API: `text` for scroll-text, `media` for media (see player.controller formatLayoutResponse). */
    val type: String? = null,
    val sectionType: String? = null,
    val hasTextConfig: Boolean? = null,
    val textConfig: ScrollTextConfigDto? = null,
    val items: List<LayoutItemDto> = emptyList()
) : Parcelable

fun LayoutSectionDto.isScrollTextSection(): Boolean {
    if (type.equals("text", ignoreCase = true)) return true
    if (sectionType.equals("SCROLL_TEXT", ignoreCase = true)) return true
    // Legacy CMS rows: textConfig present but sectionType still MEDIA
    val tc = textConfig
    if (tc != null && tc.text.isNotBlank()) return true
    return false
}

@Parcelize
data class LayoutItemDto(
    val id: String,
    val order: Int,
    val duration: Int? = null, // seconds
    val orientation: String? = null, // LANDSCAPE or PORTRAIT
    val resizeMode: String? = "FIT", // FIT, FILL, STRETCH
    val rotation: Int? = 0, // 0, 90, 180, 270
    val media: MediaDto
) : Parcelable