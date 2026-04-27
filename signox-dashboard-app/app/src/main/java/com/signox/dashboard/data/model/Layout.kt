package com.signox.dashboard.data.model

import com.google.gson.annotations.SerializedName

// Layout data models
data class Layout(
    val id: String,
    val name: String,
    val description: String?,
    val width: Int,
    val height: Int,
    val orientation: String, // LANDSCAPE or PORTRAIT
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val createdById: String?,
    val sections: List<LayoutSection>? = null,
    @SerializedName("_count")
    val count: LayoutCount? = null
)

data class LayoutCount(
    val sections: Int,
    val displays: Int
)

data class LayoutSection(
    val id: String,
    val layoutId: String,
    val name: String,
    val order: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val frequency: Int?,
    val loopEnabled: Boolean,
    val items: List<LayoutSectionItem>? = null
)

data class LayoutSectionItem(
    val id: String,
    val sectionId: String,
    val mediaId: String,
    val order: Int,
    val duration: Int?,
    val orientation: String?,
    val resizeMode: String, // FIT, FILL, STRETCH
    val rotation: Int, // 0, 90, 180, 270
    val media: Media?
)

data class MediaInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("fileSize") val fileSize: Int?
)

// API Response models
data class LayoutsResponse(
    val layouts: List<Layout>
)

data class LayoutResponse(
    val layout: Layout
)

data class DeleteLayoutResponse(
    val message: String
)

// Request models
data class CreateLayoutRequest(
    val name: String,
    val description: String? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val orientation: String? = null,
    val sections: List<CreateSectionRequest>? = null
)

data class CreateSectionRequest(
    val name: String,
    val order: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val frequency: Int? = null,
    val loopEnabled: Boolean = true,
    val items: List<CreateSectionItemRequest>? = null
)

data class CreateSectionItemRequest(
    val mediaId: String,
    val order: Int,
    val duration: Int? = null,
    val orientation: String? = null,
    val resizeMode: String = "FIT",
    val rotation: Int = 0
)

data class UpdateLayoutRequest(
    val name: String? = null,
    val description: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val orientation: String? = null,
    val isActive: Boolean? = null
)

data class UpdateSectionRequest(
    val name: String? = null,
    val frequency: Int? = null,
    val loopEnabled: Boolean? = null,
    val items: List<CreateSectionItemRequest>? = null
)

// Layout templates
data class LayoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val thumbnail: Int, // drawable resource
    val width: Int,
    val height: Int,
    val orientation: String,
    val sections: List<TemplateSectionConfig>
)

data class TemplateSectionConfig(
    val name: String,
    val order: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
