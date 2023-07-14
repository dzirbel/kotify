package com.dzirbel.kotify.network.model

/**
 * Common interface for properties found in most Spotify network objects.
 *
 * TODO refactor UI to avoid direct use of (unstable) network objects
 */
interface SpotifyObject {
    /** The Spotify ID for the object. */
    val id: String?

    /** A link to the Web API endpoint providing full details of the object. */
    val href: String?

    /** The name of the object. In case of a takedown, the value may be an empty string. */
    val name: String?

    /** The object type. */
    val type: String

    /** The Spotify URI for the object. */
    val uri: String?
}
