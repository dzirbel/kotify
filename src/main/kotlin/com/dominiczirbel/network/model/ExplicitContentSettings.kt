package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#explicit-content-settings-object
 */
data class ExplicitContentSettings(
    /** When true, indicates that explicit content should not be played. */
    val filterEnabled: Boolean,

    /** When true, indicates that the explicit content setting is locked and can’t be changed by the user. */
    val filterLocked: Boolean
)
