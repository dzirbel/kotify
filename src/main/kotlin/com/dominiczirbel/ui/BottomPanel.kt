package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.TrackPlayback
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.RefreshButton
import com.dominiczirbel.ui.common.SimpleTextButton
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val ALBUM_ART_SIZE = 75.dp

@Composable
fun BottomPanel() {
    val events = remember { MutableSharedFlow<Unit>() }
    val loading = remember { mutableStateOf(true) }
    val state = RemoteState.of(sharedFlow = events) {
        Spotify.Player.getCurrentlyPlayingTrack()
            .also { loading.value = false }
    }

    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.current.surface2)
                .padding(Dimens.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerState(state = state)

            PlayerControls(state = state, events = events)

            RefreshButton(
                refreshing = loading,
                onClick = {
                    runBlocking { events.emit(Unit) }
                },
                content = { }
            )
        }
    }
}

@Composable
private fun PlayerState(state: RemoteState<TrackPlayback?>) {
    Row {
        if (state is RemoteState.Error) {
            Text("Error: ${state.throwable.message}", color = Colors.current.error)
        } else {
            val track = (state as? RemoteState.Success)?.data?.item

            LoadedImage(
                url = track?.album?.images?.firstOrNull()?.url,
                size = ALBUM_ART_SIZE
            )

            Spacer(Modifier.size(Dimens.space3))

            track?.let {
                Column {
                    Text(track.name)
                    Spacer(Modifier.size(Dimens.space2))
                    Text(track.artists.joinToString { it.name })
                    Spacer(Modifier.size(Dimens.space2))
                    Text(track.album.name)
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(state: RemoteState<TrackPlayback?>, events: MutableSharedFlow<Unit>) {
    val controlsEnabled = state is RemoteState.Success

    val togglingPlayback = remember { mutableStateOf(false) }

    val playing = (state as? RemoteState.Success)?.data?.isPlaying

    SimpleTextButton(
        enabled = controlsEnabled && !togglingPlayback.value,
        onClick = {
            togglingPlayback.value = true
            GlobalScope.launch {
                if (playing == true) {
                    Spotify.Player.pausePlayback()
                } else {
                    Spotify.Player.startPlayback()
                }

                events.emit(Unit)

                // TODO ideally wouldn't reset until the new TrackPlayback state is loaded
                togglingPlayback.value = false
            }
        }
    ) {
        Text(
            when {
                togglingPlayback.value -> "..."
                playing == true -> "Pause"
                else -> "Play"
            }
        )
    }
}
