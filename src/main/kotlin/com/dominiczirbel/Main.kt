package com.dominiczirbel

import com.dominiczirbel.network.Spotify
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
    Secrets.load()

    FuelManager.instance.addRequestInterceptor { transformer ->
        { request ->
            println(">> ${request.method} ${request.url}")
            transformer(request)
        }
    }

    FuelManager.instance.addResponseInterceptor { transformer ->
        { request, response ->
            println("<< ${response.statusCode} ${request.method} ${response.url}")
            transformer(request, response)
        }
    }

    Secrets["track-id"]?.let {
        val (track, duration) = measureTimedValue { runBlocking { Spotify.getTrack(it) } }
        println()
        println("Track lookup succeeded in $duration:")
        println("  track name: ${track.name}")
        println("  track duration: ${track.durationMs}ms")
        println("  album name: ${track.album.name}")
        println("  released: ${track.album.releaseDate}")
        println("  artists: ${track.artists.map { it.name }}")
    }
}
