package com.dzirbel.kotify

import com.dzirbel.kotify.network.Spotify

/**
 * Temporarily sets the [Spotify.configuration] to [configuration], runs [block], and then resets the
 * [Spotify.configuration] to its previous value.
 */
fun withSpotifyConfiguration(configuration: Spotify.Configuration, block: () -> Unit) {
    val oldConfig = Spotify.configuration
    Spotify.configuration = configuration

    try {
        block()
    } finally {
        Spotify.configuration = oldConfig
    }
}
