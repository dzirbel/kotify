package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#artist-object-simplified
 */
data class SimplifiedArtist(
    /**
     * Known external URLs for this artist.
     */
    @SerializedName("external_urls") val externalUrls: Map<String, String>,

    /**
     * A link to the Web API endpoint providing full details of the artist.
     */
    val href: String,

    /**
     * The Spotify ID for the artist.
     */
    val id: String,

    /**
     * The name of the artist.
     */
    val name: String,

    /**
     * The object type: "artist"
     */
    val type: String,

    /**
     * The Spotify URI for the artist.
     */
    val uri: String
)
