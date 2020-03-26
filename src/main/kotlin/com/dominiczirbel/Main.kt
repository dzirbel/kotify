package com.dominiczirbel

import com.dominiczirbel.network.Spotify
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
    Secrets.load()
    Secrets.authenticate()

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

    Secrets["track_id"]?.let {
        trackLookup(it)
        trackLookup(it)
        tracksLookup(listOf(it, it))
    }
}

@ExperimentalTime
private fun trackLookup(id: String) {
    val (track, duration) = measureTimedValue { runBlocking { Spotify.Tracks.getTrack(id) } }
    println()
    println("Track lookup for $id succeeded in $duration:")
    println("  track name: ${track.name}")
    println("  track duration: ${track.durationMs}ms")
    println("  album name: ${track.album.name}")
    println("  released date: ${track.album.releaseDate}")
    println("  artists: ${track.artists.map { it.name }}")
    println()
}

@ExperimentalTime
private fun tracksLookup(ids: List<String>) {
    val (tracks, duration) = measureTimedValue { runBlocking { Spotify.Tracks.getTracks(ids) } }
    println()
    println("Track lookups for $ids succeeded in $duration:")
    println("  track names: ${tracks.map { it.name }}")
    println("  track durations: ${tracks.map { it.durationMs }}ms")
    println("  album names: ${tracks.map { it.album.name }}")
    println("  released dates: ${tracks.map { it.album.releaseDate }}")
    println("  artists: ${tracks.map { track -> track.artists.map { it.name }}}")
    println()
}
