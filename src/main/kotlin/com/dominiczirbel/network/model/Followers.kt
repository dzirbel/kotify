package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#followers-object
 */
data class Followers(
    /**
     * A link to the Web API endpoint providing full details of the followers; null if not available. Please note that
     * this will always be set to null, as the Web API does not support it at the moment.
     */
    val href: String,

    /** The total number of followers. */
    val total: Int
)
