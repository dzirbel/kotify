package com.dominiczirbel.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.FullTrack
import kotlinx.coroutines.launch

data class TrackLookupViewModel(
    val trackId: String = "2TpxZ7JUBn3uw46aR7qd6V",
    val loading: Boolean = false,
    val track: FullTrack? = null
)

@Composable
fun ColumnScope.TrackLookup() {
    val viewModel = remember { mutableStateOf(TrackLookupViewModel()) }
    val coroutineScope = rememberCoroutineScope()

    Text("Look up track by ID")

    TextField(
        value = viewModel.value.trackId,
        onValueChange = { viewModel.value = viewModel.value.copy(trackId = it) },
        label = {
            Text("Track ID")
        }
    )

    LoadingButton(
        enabled = viewModel.value.trackId.isNotEmpty(),
        modifier = Modifier.align(Alignment.End),
        loading = viewModel.value.loading,
        onClick = {
            viewModel.value = viewModel.value.copy(loading = true)

            coroutineScope.launch {
                val result = runCatching {
                    Spotify.Tracks.getTrack(viewModel.value.trackId)
                }
                viewModel.value = viewModel.value.copy(loading = false, track = result.getOrNull())
            }
        }
    ) {
        Text("Look up")
    }

    viewModel.value.track?.let { track ->
        Text("Track lookup for ${track.id} succeeded")
        Text("  track name: ${track.name}")
        Text("  track duration: ${track.durationMs}ms")
        Text("  album name: ${track.album.name}")
        Text("  released date: ${track.album.releaseDate}")
        Text("  artists: ${track.artists.map { it.name }}")
    }
}
