package com.dzirbel.kotify.network.model

import com.dzirbel.kotify.network.SimplifiedSpotifyTrackOrEpisode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylistTrack(
    /**
     * The date and time the track or episode was added.
     * Note that some very old playlists may return null in this field.
     */
    @SerialName("added_at") val addedAt: String?,

    /**
     * The Spotify user who added the track or episode.
     * Note that some very old playlists may return null in this field.
     */
    @SerialName("added_by") val addedBy: PublicSpotifyUser?,

    /** Whether this track or episode is a local file or not. */
    @SerialName("is_local") val isLocal: Boolean,

    /** Information about the track or episode. */
    @SerialName("track") val track: SimplifiedSpotifyTrackOrEpisode?,

    /** Undocumented field. */
    @SerialName("primary_color") val primaryColor: String? = null,

    /** Undocumented field. */
    @SerialName("video_thumbnail") val videoThumbnail: Map<String, String?>? = null,
)
