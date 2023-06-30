package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyExternalUrl(
    /** The Spotify URL for the object. */
    val spotify: String? = null,
)
