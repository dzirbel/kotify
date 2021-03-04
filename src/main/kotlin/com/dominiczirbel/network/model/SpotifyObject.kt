package com.dominiczirbel.network.model

import com.dominiczirbel.cache.CacheableObject

/**
 * Common interface for properties found in most Spotify network objects.
 */
interface SpotifyObject : CacheableObject {
    /** The Spotify ID for the object. */
    override val id: String?

    /** A link to the Web API endpoint providing full details of the object. */
    val href: String?

    /** The name of the object. In case of a takedown, the value may be an empty string. */
    val name: String

    /** The object type. */
    val type: String

    /** The Spotify URI for the object. */
    val uri: String?
}
