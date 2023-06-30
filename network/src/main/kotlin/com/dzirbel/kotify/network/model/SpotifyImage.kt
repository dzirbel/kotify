package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyImage(
    /** The image height in pixels. If unknown: null or not returned. */
    val height: Int? = null,

    /** The source URL of the image. */
    val url: String,

    /** The image width in pixels. If unknown: null or not returned. */
    val width: Int? = null,
)
