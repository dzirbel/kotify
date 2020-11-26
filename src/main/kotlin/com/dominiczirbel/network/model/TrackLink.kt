package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#track-link
 */
data class TrackLink(
    /**
     * Known external URLs for this track.
     */
    val externalUrls: Map<String, String>,

    /**
     * A link to the Web API endpoint providing full details of the track.
     */
    val href: String,

    /**
     * The Spotify ID for the track.
     */
    val id: String,

    /**
     * The object type: "track".
     */
    val type: String,

    /**
     * The Spotify URI for the track.
     */
    val uri: String
)
