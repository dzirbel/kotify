package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Undocumented object.
 */
@Serializable
data class SpotifyPlayback(
    val timestamp: Long,
    val device: SpotifyPlaybackDevice,
    @SerialName("progress_ms") val progressMs: Long,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("currently_playing_type") val currentlyPlayingType: String,
    val item: FullSpotifyTrack?,
    @SerialName("shuffle_state") val shuffleState: Boolean,
    @SerialName("repeat_state") val repeatState: String,
    val context: SpotifyPlaybackContext?,
    val actions: JsonObject? = null
)

/**
 * Undocumented object.
 */
@Serializable
data class SpotifyTrackPlayback(
    val timestamp: Long,
    @SerialName("progress_ms") val progressMs: Long,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("currently_playing_type") val currentlyPlayingType: String,
    val item: FullSpotifyTrack?,
    val context: SpotifyPlaybackContext?,
    val actions: JsonObject? = null
)

/**
 * Undocumented object.
 */
@Serializable
data class SpotifyPlaybackDevice(
    val id: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_restricted") val isRestricted: Boolean,
    @SerialName("is_private_session") val isPrivateSession: Boolean?,
    val name: String,
    val type: String,
    @SerialName("volume_percent") val volumePercent: Int
)

/**
 * Undocumented object.
 */
@Serializable
data class SpotifyPlaybackContext(
    val uri: String,
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrl,
    val href: String,
    val type: String
)

/**
 * Undocumented object.
 */
@Serializable
data class SpotifyPlayHistoryObject(
    val track: FullSpotifyTrack,
    @SerialName("played_at") val playedAt: String,
    val context: SpotifyPlaybackContext
)
