package com.dominiczirbel

import com.dominiczirbel.network.Spotify

fun withSpotifyConfiguration(configuration: Spotify.Configuration, block: () -> Unit) {
    val oldConfig = Spotify.configuration
    Spotify.configuration = configuration

    try {
        block()
    } finally {
        Spotify.configuration = oldConfig
    }
}
