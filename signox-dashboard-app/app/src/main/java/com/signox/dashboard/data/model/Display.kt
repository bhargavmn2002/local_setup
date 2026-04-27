package com.signox.dashboard.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Display(
    val id: String,
    val name: String,
    val status: String, // ONLINE, OFFLINE, PAIRING, ERROR
    val isPaired: Boolean,
    val pairingCode: String?,
    val deviceToken: String?,
    val lastHeartbeat: String?,
    val lastSeenAt: String?,
    val pairedAt: String?,
    val location: String?,
    val orientation: String?,
    val tags: List<String>?,
    val playlistId: String?,
    val layoutId: String?,
    val managedByUserId: String?,
    val clientAdminId: String?,
    val kioskModeEnabled: Boolean?,
    val createdAt: String,
    val updatedAt: String,
    val playlist: PlaylistInfo?,
    val layout: LayoutInfo?,
    val managedByUser: UserInfo?,
    val clientAdmin: UserInfo?,
    val activeSchedule: ActiveScheduleInfo?
) : Parcelable

@Parcelize
data class PlaylistInfo(
    val id: String,
    val name: String
) : Parcelable

@Parcelize
data class LayoutInfo(
    val id: String,
    val name: String
) : Parcelable

@Parcelize
data class UserInfo(
    val id: String,
    val email: String,
    val role: String
) : Parcelable

@Parcelize
data class ActiveScheduleInfo(
    val id: String,
    val name: String,
    val priority: Int,
    val contentType: String,
    val contentName: String?
) : Parcelable

data class DisplaysResponse(
    val displays: List<Display>
)

data class PairingCodeResponse(
    val pairingCode: String,
    val displayId: String,
    val code: String?,
    val id: String?
)

data class PairDisplayRequest(
    val pairingCode: String,
    val name: String,
    val managedByUserId: String?
)

data class PairDisplayResponse(
    val success: Boolean,
    val message: String,
    val display: Display,
    val deviceToken: String
)

data class UpdateDisplayRequest(
    val name: String?,
    val playlistId: String?,
    val layoutId: String?,
    val location: String?,
    val orientation: String?,
    val tags: List<String>?
)

data class DeleteDisplayResponse(
    val message: String
)
