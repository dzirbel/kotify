package com.dzirbel.kotify.network.model

import androidx.compose.runtime.Immutable

/**
 * Common interface for properties found in most Spotify network objects.
 */
@Immutable
interface SpotifyObject {
    /** The Spotify ID for the object. */
    val id: String?

    /** A link to the Web API endpoint providing full details of the object. */
    val href: String?

    /** The name of the object. In case of a takedown, the value may be an empty string. */
    val name: String

    /** The object type. */
    val type: String

    /** The Spotify URI for the object. */
    val uri: String?
}
