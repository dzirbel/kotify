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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.Playback
import com.dominiczirbel.network.model.TrackPlayback
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.RefreshButton
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val ALBUM_ART_SIZE = 75.dp

private class BottomPanelPresenter : Presenter<
    BottomPanelPresenter.State,
    BottomPanelPresenter.Event,
    BottomPanelPresenter.Result>(startingEvents = listOf(Event.Load())) {

    data class State(
        val loadingPlayback: Boolean = false,
        val playback: Playback? = null,
        val loadingTrackPlayback: Boolean = false,
        val trackPlayback: TrackPlayback? = null
    )

    sealed class Event {
        data class Load(val loadPlayback: Boolean = true, val loadTrackPlayback: Boolean = true) : Event()
    }

    sealed class Result {
        data class Loading(val loadingPlayback: Boolean = true, val loadingTrackPlayback: Boolean = true) : Result()
        data class PlaybackLoaded(val playback: Playback?) : Result()
        data class TrackPlaybackLoaded(val trackPlayback: TrackPlayback?) : Result()
    }

    override val initialState: State = State()

    override fun reactTo(event: Event): Flow<Result> {
        return when (event) {
            is Event.Load -> merge(
                flow { emit(Result.TrackPlaybackLoaded(Spotify.Player.getCurrentlyPlayingTrack())) },
                flow { emit(Result.PlaybackLoaded(Spotify.Player.getCurrentPlayback())) }
            ).onStart { emit(Result.Loading()) }
        }
    }

    override fun apply(state: State, result: Result): State {
        return when (result) {
            is Result.Loading -> state.copy(
                loadingPlayback = state.loadingPlayback || result.loadingPlayback,
                loadingTrackPlayback = state.loadingTrackPlayback || result.loadingTrackPlayback
            )
            is Result.PlaybackLoaded -> state.copy(playback = result.playback, loadingPlayback = false)
            is Result.TrackPlaybackLoaded -> state.copy(
                trackPlayback = result.trackPlayback,
                loadingTrackPlayback = false
            )
        }
    }
}

@Composable
fun BottomPanel() {
    val presenter = remember { BottomPanelPresenter() }
    val state = presenter.state()

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

            PlayerControls(remoteState = state, events = presenter.events)

            RefreshButton(
                refreshing = (state as? RemoteState.Success)
                    ?.data
                    ?.let { it.loadingPlayback || it.loadingTrackPlayback } == true,
                onClick = {
                    runBlocking { presenter.events.emit(BottomPanelPresenter.Event.Load()) }
                },
                content = { }
            )
        }
    }
}

@Composable
private fun PlayerState(state: RemoteState<BottomPanelPresenter.State>) {
    Row {
        if (state is RemoteState.Error) {
            Text("Error: ${state.throwable.message}", color = Colors.current.error)
        } else {
            val track = (state as? RemoteState.Success)?.data?.trackPlayback?.item

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
private fun PlayerControls(
    remoteState: RemoteState<BottomPanelPresenter.State>,
    events: MutableSharedFlow<BottomPanelPresenter.Event>
) {
    val state = (remoteState as? RemoteState.Success)?.data

    val controlsEnabled = state?.loadingPlayback == false && state.playback != null

    val playing = state?.playback?.isPlaying
    val shuffling = state?.playback?.shuffleState
    val repeatState = state?.playback?.repeatState

    Row(verticalAlignment = Alignment.CenterVertically) {
        val togglingShuffle = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !togglingShuffle.value,
            onClick = {
                togglingShuffle.value = true

                // TODO use coroutine scope context
                GlobalScope.launch {
                    Spotify.Player.toggleShuffle(state = !shuffling!!)

                    events.emit(BottomPanelPresenter.Event.Load(loadPlayback = true, loadTrackPlayback = false))

                    togglingShuffle.value = false
                }
            }
        ) {
            Icon(
                painter = svgResource("shuffle.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                tint = if (shuffling == true) {
                    MaterialTheme.colors.primary.copy(alpha = LocalContentAlpha.current)
                } else {
                    LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                },
                contentDescription = "Shuffle"
            )
        }

        val skippingPrevious = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !skippingPrevious.value,
            onClick = {
                skippingPrevious.value = true

                // TODO use coroutine scope context
                GlobalScope.launch {
                    Spotify.Player.skipToPrevious()

                    events.emit(BottomPanelPresenter.Event.Load(loadPlayback = true, loadTrackPlayback = false))

                    skippingPrevious.value = false
                }
            }
        ) {
            Icon(
                painter = svgResource("skip-previous.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = "Previous"
            )
        }

        val togglingPlayback = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !togglingPlayback.value,
            onClick = {
                togglingPlayback.value = true

                // TODO use coroutine scope context
                GlobalScope.launch {
                    if (playing!!) {
                        Spotify.Player.pausePlayback()
                    } else {
                        Spotify.Player.startPlayback()
                    }

                    events.emit(BottomPanelPresenter.Event.Load(loadPlayback = true, loadTrackPlayback = false))

                    togglingPlayback.value = false
                }
            }
        ) {
            Icon(
                painter = svgResource(if (playing == false) "play-circle-outline.svg" else "pause-circle-outline.svg"),
                modifier = Modifier.size(Dimens.iconMedium),
                contentDescription = if (playing == false) "Play" else "Pause"
            )
        }

        val skippingNext = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !skippingNext.value,
            onClick = {
                skippingNext.value = true

                // TODO use coroutine scope context
                GlobalScope.launch {
                    Spotify.Player.skipToNext()

                    events.emit(BottomPanelPresenter.Event.Load(loadPlayback = true, loadTrackPlayback = false))

                    skippingNext.value = false
                }
            }
        ) {
            Icon(
                painter = svgResource("skip-next.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = "Next"
            )
        }

        val togglingRepeat = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !togglingRepeat.value,
            onClick = {
                togglingRepeat.value = true

                // TODO use coroutine scope context
                GlobalScope.launch {
                    Spotify.Player.setRepeatMode(
                        state = when (repeatState) {
                            "track" -> "off"
                            "context" -> "track"
                            else -> "context"
                        }
                    )

                    events.emit(BottomPanelPresenter.Event.Load(loadPlayback = true, loadTrackPlayback = false))

                    togglingRepeat.value = false
                }
            }
        ) {
            Icon(
                painter = svgResource(if (repeatState == "track") "repeat-one.svg" else "repeat.svg"),
                tint = if (repeatState == "track" || repeatState == "context") {
                    MaterialTheme.colors.primary.copy(alpha = LocalContentAlpha.current)
                } else {
                    LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                },
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = "Repeat"
            )
        }
    }
}
