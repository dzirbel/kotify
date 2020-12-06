package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#playlist-track-object
 */
data class PlaylistTrack(
    /**
     * The date and time the track or episode was added.
     * Note that some very old playlists may return null in this field.
     */
    val addedAt: String?,

    /**
     * The Spotify user who added the track or episode.
     * Note that some very old playlists may return null in this field.
     */
    val addedBy: PublicUser,

    /** Whether this track or episode is a local file or not. */
    val isLocal: Boolean,

    /** Information about the track or episode. */
    val track: SimplifiedTrack, // TODO might be episode object instead

    /** Undocumented field. */
    val primaryColor: String?,

    /** Undocumented field. */
    val videoThumbnail: Map<String, String>?
)
