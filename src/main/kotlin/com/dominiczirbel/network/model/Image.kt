package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-imageobject
 */
@Serializable
data class Image(
    /** The image height in pixels. If unknown: null or not returned. */
    val height: Int? = null,

    /** The source URL of the image. */
    val url: String,

    /** The image width in pixels. If unknown: null or not returned. */
    val width: Int? = null
)
