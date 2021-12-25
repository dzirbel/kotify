package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-categoryobject
 */
@Serializable
data class SpotifyCategory(
    /** A link to the Web API endpoint returning full details of the category. */
    val href: String,

    /** The category icon, in various sizes. */
    val icons: List<SpotifyImage>,

    /** The Spotify category ID of the category. */
    val id: String,

    /** The name of the category. */
    val name: String
)
