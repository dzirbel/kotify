package com.dominiczirbel.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Undocumented object.
 */
@Serializable
data class Playback(
    val timestamp: Long,
    val device: PlaybackDevice,
    @SerialName("progress_ms") val processMs: Int,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("currently_playing_type") val currentlyPlayingType: String,
    val item: JsonObject,
    @SerialName("shuffle_state") val shuffleState: Boolean,
    @SerialName("repeat_state") val repeatState: String,
    val context: PlaybackContext,
    val actions: JsonObject? = null
)

/**
 * Undocumented object.
 */
@Serializable
data class TrackPlayback(
    val timestamp: Long,
    @SerialName("progress_ms") val progressMs: Long,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("currently_playing_type") val currentlyPlayingType: String,
    val item: FullTrack,
    val context: PlaybackContext,
    val actions: JsonObject? = null
)

/**
 * Undocumented object.
 */
@Serializable
data class PlaybackDevice(
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
data class PlaybackContext(
    val uri: String,
    @SerialName("external_urls") val externalUrls: ExternalUrl,
    val href: String,
    val type: String
)

/**
 * Undocumented object.
 */
@Serializable
data class PlayHistoryObject(
    val track: FullTrack,
    @SerialName("played_at") val playedAt: String,
    val context: PlaybackContext
)
