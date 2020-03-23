package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#image-object
 */
data class Image(
    /**
     * The image height in pixels. If unknown: null or not returned.
     */
    val height: Int?,

    /**
     * The source URL of the image.
     */
    val url: String,

    /**
     * The image width in pixels. If unknown: null or not returned.
     */
    val width: Int?
)
