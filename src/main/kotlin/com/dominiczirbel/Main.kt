package com.dominiczirbel

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.DesktopDialogProperties
import androidx.compose.ui.window.Dialog
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.ui.AuthenticationView
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
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

    // TODO integrate with UI
    Secrets["track_id"]?.let {
        trackLookup(it)
        trackLookup(it)
        tracksLookup(listOf(it, it))
    }

    @Suppress("MagicNumber")
    Window(title = "Spotify Client") {
        MaterialTheme {
            val authenticating = remember { mutableStateOf<Boolean?>(true) }
            if (authenticating.value == true) {
                Text("Authenticating...")
                Dialog(
                    properties = DesktopDialogProperties(
                        title = "Spotify API Authentication",
                        size = IntSize(400, 400)
                    ),
                    onDismissRequest = { authenticating.value = null }
                ) {
                    AuthenticationView(onAuthenticated = { authenticating.value = false })
                }
            } else {
                Column {
                    Text(if (authenticating.value == false) "Authenticated!" else "Canceled authentication")
                    Button(
                        onClick = { authenticating.value = true }
                    ) {
                        Text("Authenticate again?")
                    }
                }
            }
        }
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
    println("  artists: ${tracks.map { track -> track.artists.map { it.name } }}")
    println()
}
