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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.Playback
import com.dominiczirbel.network.model.PlaybackDevice
import com.dominiczirbel.network.model.TrackPlayback
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.SeekableSlider
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.util.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private val ALBUM_ART_SIZE = 75.dp
private val TRACK_SLIDER_WIDTH = 1_000.dp
private val VOLUME_SLIDER_WIDTH = 100.dp

// TODO sometimes on skip/etc the immediate call to playback returns outdated info, from before the skip/etc has been
//  applied - keep making calls until the playback is updated?
private class BottomPanelPresenter(scope: CoroutineScope) :
    Presenter<BottomPanelPresenter.State, BottomPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load.all),
        eventMergeStrategy = EventMergeStrategy.LATEST,
        initialState = State()
    ) {

    init {
        scope.launch {
            Player.playEvents.collect {
                events.emit(Event.Load(loadPlayback = true, loadTrackPlayback = true, loadDevices = false))
            }
        }
    }

    data class State(
        val loadingPlayback: Boolean = false,
        val playback: Playback? = null,
        val loadingTrackPlayback: Boolean = false,
        val trackPlayback: TrackPlayback? = null,
        val loadingDevices: Boolean = false,
        val devices: List<PlaybackDevice>? = null
    )

    sealed class Event {
        data class Load(
            val loadPlayback: Boolean,
            val loadTrackPlayback: Boolean,
            val loadDevices: Boolean
        ) : Event() {
            companion object {
                val playback = Load(loadPlayback = true, loadTrackPlayback = false, loadDevices = false)
                val trackPlayback = Load(loadPlayback = false, loadTrackPlayback = true, loadDevices = false)
                val devices = Load(loadPlayback = false, loadTrackPlayback = false, loadDevices = true)
                val all = Load(loadPlayback = true, loadTrackPlayback = true, loadDevices = true)
            }
        }
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState {
                    it.copy(
                        loadingPlayback = event.loadPlayback,
                        loadingTrackPlayback = event.loadTrackPlayback,
                        loadingDevices = event.loadDevices
                    )
                }

                if (event.loadPlayback) {
                    scope.launch {
                        val playback = Spotify.Player.getCurrentPlayback()

                        Player.playable.value = playback?.device != null

                        mutateState {
                            it.copy(playback = playback, loadingPlayback = false)
                        }
                    }
                }

                if (event.loadDevices) {
                    scope.launch {
                        val devices = Spotify.Player.getAvailableDevices()
                        mutateState {
                            it.copy(devices = devices, loadingDevices = false)
                        }
                    }
                }

                if (event.loadTrackPlayback) {
                    scope.launch {
                        val trackPlayback = Spotify.Player.getCurrentlyPlayingTrack()
                        if (trackPlayback == null) {
                            mutateState { it.copy(loadingTrackPlayback = false, trackPlayback = null) }
                        } else {
                            if (trackPlayback.item == null) {
                                // try again until we get a valid track
                                delay(REFRESH_BUFFER_MS)
                                events.emit(
                                    Event.Load(loadTrackPlayback = true, loadPlayback = false, loadDevices = false)
                                )
                            } else {
                                mutateState {
                                    it.copy(trackPlayback = trackPlayback, loadingTrackPlayback = false)
                                }

                                // refresh after the current track is expected to end
                                if (trackPlayback.isPlaying) {
                                    val millisLeft = trackPlayback.item.durationMs - trackPlayback.progressMs

                                    delay(millisLeft + REFRESH_BUFFER_MS)

                                    events.emit(
                                        Event.Load(loadTrackPlayback = true, loadPlayback = true, loadDevices = false)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * A buffer in milliseconds after the current track is expected to end before fetching the next playback object,
         * to account for network time, etc.
         */
        private const val REFRESH_BUFFER_MS = 500L
    }
}

@Composable
fun BottomPanel() {
    val scope = rememberCoroutineScope()
    val presenter = remember { BottomPanelPresenter(scope) }
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
            // TODO control/progress jump around when the size of the PlayerState changes
            PlayerState(state = state)

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                PlayerControls(state = state, events = presenter.events)

                TrackProgress(state = state.trackPlayback, events = presenter.events)
            }

            val refreshing = state.loadingDevices || state.loadingPlayback || state.loadingTrackPlayback

            Row(verticalAlignment = Alignment.CenterVertically) {
                // TODO volume slider often buggy - might be fetching device state before new volume has been applied
                val devices = state.devices
                val currentDevice = devices?.firstOrNull()
                SeekableSlider(
                    progress = @Suppress("MagicNumber") currentDevice?.volumePercent?.let { it.toFloat() / 100 },
                    dragKey = currentDevice,
                    sliderWidth = VOLUME_SLIDER_WIDTH,
                    leftContent = {
                        Icon(
                            painter = svgResource("volume-up.svg"),
                            contentDescription = "Volume"
                        )
                    },
                    onSeek = { seekPercent ->
                        scope.launch {
                            Spotify.Player.setVolume(
                                volumePercent = @Suppress("MagicNumber") (seekPercent * 100).roundToInt()
                            )

                            presenter.events.emit(BottomPanelPresenter.Event.Load.devices)
                        }
                    }
                )

                IconButton(
                    enabled = !refreshing,
                    onClick = {
                        presenter.emitEvent(BottomPanelPresenter.Event.Load.all)
                    }
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(Dimens.iconMedium))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(Dimens.iconMedium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerState(state: BottomPanelPresenter.State) {
    Row {
        val track = state.trackPlayback?.item

        LoadedImage(
            url = track?.album?.images?.firstOrNull()?.url,
            size = ALBUM_ART_SIZE,
            scope = rememberCoroutineScope()
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

@Composable
private fun PlayerControls(
    state: BottomPanelPresenter.State,
    events: MutableSharedFlow<BottomPanelPresenter.Event>
) {
    val scope = rememberCoroutineScope()

    val controlsEnabled = !state.loadingPlayback && state.playback != null

    val playing = state.playback?.isPlaying
    val shuffling = state.playback?.shuffleState
    val repeatState = state.playback?.repeatState

    Row(verticalAlignment = Alignment.CenterVertically) {
        val togglingShuffle = remember { mutableStateOf(false) }
        IconButton(
            enabled = controlsEnabled && !togglingShuffle.value,
            onClick = {
                togglingShuffle.value = true

                scope.launch {
                    Spotify.Player.toggleShuffle(state = !shuffling!!)

                    events.emit(BottomPanelPresenter.Event.Load.playback)

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

                scope.launch {
                    Spotify.Player.skipToPrevious()

                    events.emit(BottomPanelPresenter.Event.Load.trackPlayback)

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

                scope.launch {
                    if (playing!!) {
                        Spotify.Player.pausePlayback()
                    } else {
                        Spotify.Player.startPlayback()
                    }

                    events.emit(BottomPanelPresenter.Event.Load.playback)

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

                scope.launch {
                    Spotify.Player.skipToNext()

                    events.emit(BottomPanelPresenter.Event.Load.trackPlayback)

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

                scope.launch {
                    Spotify.Player.setRepeatMode(
                        state = when (repeatState) {
                            "track" -> "off"
                            "context" -> "track"
                            else -> "context"
                        }
                    )

                    events.emit(BottomPanelPresenter.Event.Load.playback)

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

@Composable
private fun TrackProgress(state: TrackPlayback?, events: MutableSharedFlow<BottomPanelPresenter.Event>) {
    if (state == null) {
        SeekableSlider(progress = null, sliderWidth = TRACK_SLIDER_WIDTH)
    } else {
        val scope = rememberCoroutineScope()
        val progressState = if (state.isPlaying) {
            remember(state) {
                flow {
                    val start = System.nanoTime()
                    while (true) {
                        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                        emit(state.progressMs + elapsedMs)
                        delay(TimeUnit.SECONDS.toMillis(1))
                    }
                }
            }.collectAsState(initial = state.progressMs, context = Dispatchers.IO)
        } else {
            mutableStateOf(state.progressMs)
        }

        val track = state.item!!

        // TODO animate progress more smoothly
        val progress = progressState.value.coerceAtMost(track.durationMs)

        SeekableSlider(
            progress = progress.let { progress.toFloat() / track.durationMs },
            dragKey = state,
            sliderWidth = TRACK_SLIDER_WIDTH,
            leftContent = {
                Text(text = formatDuration(progress), fontSize = Dimens.fontCaption)
            },
            rightContent = {
                Text(
                    text = remember(track.durationMs) { formatDuration(track.durationMs) },
                    fontSize = Dimens.fontCaption
                )
            },
            onSeek = { seekPercent ->
                scope.launch {
                    Spotify.Player.seekToPosition(
                        positionMs = (seekPercent * track.durationMs).roundToInt()
                    )

                    events.emit(BottomPanelPresenter.Event.Load.playback)
                }
            }
        )
    }
}
