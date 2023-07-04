package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlayback(
    /**
     * Unix Millisecond Timestamp when data was fetched.
     */
    val timestamp: Long,

    /**
     * The device that is currently active.
     */
    val device: SpotifyPlaybackDevice,

    /**
     * Progress into the currently playing track or episode. Can be null.
     */
    @SerialName("progress_ms") val progressMs: Long?,

    /**
     * If something is currently playing, return true.
     */
    @SerialName("is_playing") val isPlaying: Boolean,

    /**
     * The object type of the currently playing item. Can be one of track, episode, ad or unknown.
     */
    @SerialName("currently_playing_type") val currentlyPlayingType: String,

    /**
     * The currently playing track or episode. Can be null.
     *
     * TODO update to support episode e.g. SpotifyPlaylistTrack
     */
    val item: FullSpotifyTrack?,

    /**
     * If shuffle is on or off.
     */
    @SerialName("shuffle_state") val shuffleState: Boolean,

    /**
     * off, track, context
     */
    @SerialName("repeat_state") val repeatState: String,

    /**
     * A Context Object. Can be null.
     */
    val context: SpotifyPlaybackContext?,

    /**
     * Allows to update the user interface based on which playback actions are available within the current context.
     */
    val actions: SpotifyPlaybackActions? = null,
)

@Serializable
data class SpotifyTrackPlayback(
    val timestamp: Long,
    @SerialName("progress_ms") val progressMs: Long,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("currently_playing_type") val currentlyPlayingType: String,
    val item: FullSpotifyTrack?,
    val context: SpotifyPlaybackContext?,
    val actions: SpotifyPlaybackActions? = null,
)

@Serializable
data class SpotifyPlaybackDevice(
    /**
     * The device ID.
     */
    val id: String,

    /**
     * If this device is the currently active device.
     */
    @SerialName("is_active") val isActive: Boolean,

    /**
     * Whether controlling this device is restricted. At present if this is "true" then no Web API commands will be
     * accepted by this device.
     */
    @SerialName("is_restricted") val isRestricted: Boolean,

    /**
     * If this device is currently in a private session.
     */
    @SerialName("is_private_session") val isPrivateSession: Boolean?,

    /**
     * A human-readable name for the device. Some devices have a name that the user can configure (e.g. "Loudest
     * speaker") and some devices have a generic name associated with the manufacturer or device model.
     */
    val name: String,

    /**
     * Device type, such as "computer", "smartphone" or "speaker".
     */
    val type: String,

    /**
     * The current volume in percent.
     */
    @SerialName("volume_percent") val volumePercent: Int,
)

@Serializable
data class SpotifyPlaybackContext(
    val uri: String,
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrl,
    val href: String,
    val type: String,
)

@Serializable
data class SpotifyPlayHistoryObject(
    val track: FullSpotifyTrack,
    @SerialName("played_at") val playedAt: String,
    val context: SpotifyPlaybackContext,
)

@Serializable
data class SpotifyPlaybackOffset(val position: Int? = null)

@Serializable
data class SpotifyPlaybackActions(
    @SerialName("interrupting_playback") val interruptingPlayback: Boolean? = null,
    val pausing: Boolean? = null,
    val resuming: Boolean? = null,
    val seeking: Boolean? = null,
    @SerialName("skipping_next") val skippingNext: Boolean? = null,
    @SerialName("skipping_prev") val skippingPrev: Boolean? = null,
    @SerialName("toggling_repeat_context") val togglingRepeatContext: Boolean? = null,
    @SerialName("toggling_shuffle") val togglingShuffle: Boolean? = null,
    @SerialName("toggling_repeat_track") val togglingRepeatTrack: Boolean? = null,
    @SerialName("transferring_playback") val transferringPlayback: Boolean? = null,
    val disallows: Map<String, Boolean>? = null,
)
