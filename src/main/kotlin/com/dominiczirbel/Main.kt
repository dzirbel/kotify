package com.dominiczirbel

import com.dominiczirbel.network.Spotify
import kotlinx.coroutines.runBlocking

fun main() {
    Secrets.load()
    Secrets["track-id"]?.let {
        val track = runBlocking { Spotify.getTrack(it) }
        println(track)
    }
}
