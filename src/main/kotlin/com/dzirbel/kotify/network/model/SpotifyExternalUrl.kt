package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-externalurlobject
 */
@Serializable
data class SpotifyExternalUrl(
    /** The Spotify URL for the object. */
    val spotify: String? = null,
)
