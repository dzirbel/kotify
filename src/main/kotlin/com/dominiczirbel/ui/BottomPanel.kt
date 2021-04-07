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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.PlaybackDevice
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.SeekableSlider
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.util.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private val ALBUM_ART_SIZE = 75.dp
private val TRACK_SLIDER_WIDTH = 1_000.dp
private val VOLUME_SLIDER_WIDTH = 100.dp

// time in milliseconds between updating the track progress slider
private const val PROGRESS_SLIDER_UPDATE_DELAY_MS = 50L

private class BottomPanelPresenter(scope: CoroutineScope) :
    Presenter<BottomPanelPresenter.State, BottomPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.LoadDevices, Event.LoadPlayback, Event.LoadTrackPlayback()),
        eventMergeStrategy = EventMergeStrategy.LATEST,
        initialState = State()
    ) {

    init {
        scope.launch {
            Player.playEvents.collect {
                emit(Event.LoadPlayback)
                emit(Event.LoadTrackPlayback())
            }
        }
    }

    data class State(
        val playbackTrack: Track? = null,
        val playbackProgressMs: Long? = null,
        val playbackIsPlaying: Boolean? = null,
        val playbackShuffleState: Boolean? = null,
        val playbackRepeatState: String? = null,
        val devices: List<PlaybackDevice>? = null,

        val loadingPlayback: Boolean = false,
        val loadingTrackPlayback: Boolean = false,
        val loadingDevices: Boolean = false,

        val togglingShuffle: Boolean = false,
        val togglingRepeat: Boolean = false,
        val togglingPlayback: Boolean = false,
        val skippingPrevious: Boolean = false,
        val skippingNext: Boolean = false
    )

    sealed class Event {
        object LoadDevices : Event()
        object LoadPlayback : Event()
        class LoadTrackPlayback(val untilTrackChange: Boolean = false) : Event()

        object Play : Event()
        object Pause : Event()
        object SkipNext : Event()
        object SkipPrevious : Event()
        class ToggleShuffle(val shuffle: Boolean) : Event()
        class SetRepeat(val repeatState: String) : Event()
        class SetVolume(val volume: Int) : Event()
        class SeekTo(val positionMs: Int) : Event()
    }

    override fun reactTo(events: Flow<Event>): Flow<Event> {
        return merge(
            events.filterIsInstance<Event.LoadDevices>().transformLatest<Event, Event> { reactTo(it) },
            events.filterIsInstance<Event.LoadPlayback>().transformLatest<Event, Event> { reactTo(it) },
            events.filterIsInstance<Event.LoadTrackPlayback>().transformLatest<Event, Event> { reactTo(it) },
            events.filterIsInstance<Event.Play>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.Pause>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SkipNext>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SkipPrevious>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.ToggleShuffle>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SetRepeat>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SetVolume>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SeekTo>().transformLatest { reactTo(it) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.LoadDevices -> {
                mutateState { it.copy(loadingDevices = true) }

                val devices = runCatching { Spotify.Player.getAvailableDevices() }.getOrNull()

                mutateState { it.copy(devices = devices, loadingDevices = false) }
            }

            Event.LoadPlayback -> {
                mutateState { it.copy(loadingPlayback = true) }

                val playback = runCatching { Spotify.Player.getCurrentPlayback() }.getOrNull()

                Player.playable.value = playback?.device != null

                mutateState {
                    it.copy(
                        playbackTrack = playback?.item ?: it.playbackTrack,
                        playbackProgressMs = playback?.progressMs ?: it.playbackProgressMs,
                        playbackIsPlaying = playback?.isPlaying ?: it.playbackIsPlaying,
                        playbackShuffleState = playback?.shuffleState ?: it.playbackShuffleState,
                        playbackRepeatState = playback?.repeatState ?: it.playbackRepeatState,
                        loadingPlayback = false
                    )
                }
            }

            is Event.LoadTrackPlayback -> {
                val currentTrack: Track?
                mutateState {
                    currentTrack = it.playbackTrack
                    it.copy(loadingTrackPlayback = true)
                }

                val trackPlayback = runCatching { Spotify.Player.getCurrentlyPlayingTrack() }.getOrNull()

                when {
                    trackPlayback == null ->
                        mutateState {
                            it.copy(loadingTrackPlayback = false, playbackTrack = null)
                        }

                    trackPlayback.item == null -> {
                        // try again until we get a valid track
                        delay(REFRESH_BUFFER_MS)

                        emit(Event.LoadTrackPlayback(untilTrackChange = event.untilTrackChange))
                    }

                    event.untilTrackChange && trackPlayback.item == currentTrack -> {
                        // try again until the track changes
                        delay(REFRESH_BUFFER_MS)

                        emit(Event.LoadTrackPlayback(untilTrackChange = true))
                    }

                    else -> {
                        mutateState {
                            it.copy(
                                playbackTrack = trackPlayback.item,
                                playbackProgressMs = trackPlayback.progressMs,
                                playbackIsPlaying = trackPlayback.isPlaying,
                                loadingTrackPlayback = false
                            )
                        }

                        // refresh after the current track is expected to end
                        if (trackPlayback.isPlaying) {
                            val millisLeft = trackPlayback.item.durationMs - trackPlayback.progressMs

                            delay(millisLeft + REFRESH_BUFFER_MS)

                            emit(Event.LoadTrackPlayback())
                        }
                    }
                }
            }

            Event.Play -> {
                mutateState { it.copy(togglingPlayback = true) }

                val result = runCatching { Spotify.Player.startPlayback() }
                if (result.isSuccess) {
                    emit(Event.LoadPlayback)
                    emit(Event.LoadTrackPlayback())
                }

                mutateState { it.copy(togglingPlayback = false) }
            }

            Event.Pause -> {
                mutateState { it.copy(togglingPlayback = true) }

                val result = runCatching { Spotify.Player.pausePlayback() }
                if (result.isSuccess) {
                    emit(Event.LoadPlayback)
                    emit(Event.LoadTrackPlayback())
                }

                mutateState { it.copy(togglingPlayback = false) }
            }

            Event.SkipNext -> {
                mutateState { it.copy(skippingNext = true) }

                val result = runCatching { Spotify.Player.skipToNext() }
                if (result.isSuccess) {
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                }

                mutateState { it.copy(skippingNext = false) }
            }

            Event.SkipPrevious -> {
                mutateState { it.copy(skippingPrevious = true) }

                val result = runCatching { Spotify.Player.skipToPrevious() }
                if (result.isSuccess) {
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                }

                mutateState { it.copy(skippingPrevious = false) }
            }

            is Event.ToggleShuffle -> {
                mutateState { it.copy(togglingShuffle = true) }

                val result = runCatching { Spotify.Player.toggleShuffle(state = event.shuffle) }
                if (result.isSuccess) {
                    emit(Event.LoadPlayback)
                }

                mutateState { it.copy(togglingShuffle = false) }
            }

            is Event.SetRepeat -> {
                mutateState { it.copy(togglingRepeat = true) }

                val result = runCatching { Spotify.Player.setRepeatMode(state = event.repeatState) }
                if (result.isSuccess) {
                    emit(Event.LoadPlayback)
                }

                mutateState { it.copy(togglingRepeat = false) }
            }

            is Event.SetVolume -> {
                val result = runCatching { Spotify.Player.setVolume(volumePercent = event.volume) }
                if (result.isSuccess) {
                    emit(Event.LoadDevices)
                }
            }

            is Event.SeekTo -> {
                val result = runCatching { Spotify.Player.seekToPosition(positionMs = event.positionMs) }
                if (result.isSuccess) {
                    emit(Event.LoadPlayback)
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

    val state = presenter.state().safeState // TODO proper error handling

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
                PlayerControls(state = state, presenter = presenter)

                TrackProgress(state = state, presenter = presenter)
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
                        val volume = @Suppress("MagicNumber") (seekPercent * 100).roundToInt()
                        presenter.emitAsync(BottomPanelPresenter.Event.SetVolume(volume))
                    }
                )

                IconButton(
                    enabled = !refreshing,
                    onClick = {
                        presenter.emitAsync(
                            BottomPanelPresenter.Event.LoadDevices,
                            BottomPanelPresenter.Event.LoadPlayback,
                            BottomPanelPresenter.Event.LoadTrackPlayback()
                        )
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
        val track = state.playbackTrack
        val album = (track as? FullTrack)?.album ?: (track as? SimplifiedTrack)?.album

        LoadedImage(
            url = album?.images?.firstOrNull()?.url,
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
                Text(album?.name.orEmpty())
            }
        }
    }
}

@Composable
private fun PlayerControls(state: BottomPanelPresenter.State, presenter: BottomPanelPresenter) {
    val controlsEnabled = !state.loadingPlayback

    val playing = state.playbackIsPlaying
    val shuffling = state.playbackShuffleState
    val repeatState = state.playbackRepeatState

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = controlsEnabled && !state.togglingShuffle,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.ToggleShuffle(shuffle = !shuffling!!))
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

        IconButton(
            enabled = controlsEnabled && !state.skippingPrevious,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.SkipPrevious)
            }
        ) {
            Icon(
                painter = svgResource("skip-previous.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = "Previous"
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.togglingPlayback,
            onClick = {
                presenter.emitAsync(
                    if (playing!!) {
                        BottomPanelPresenter.Event.Pause
                    } else {
                        BottomPanelPresenter.Event.Play
                    }
                )
            }
        ) {
            Icon(
                painter = svgResource(if (playing == false) "play-circle-outline.svg" else "pause-circle-outline.svg"),
                modifier = Modifier.size(Dimens.iconMedium),
                contentDescription = if (playing == false) "Play" else "Pause"
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.skippingNext,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.SkipNext)
            }
        ) {
            Icon(
                painter = svgResource("skip-next.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = "Next"
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.togglingRepeat,
            onClick = {
                val newRepeatState = when (repeatState) {
                    "track" -> "off"
                    "context" -> "track"
                    else -> "context"
                }

                presenter.emitAsync(BottomPanelPresenter.Event.SetRepeat(repeatState = newRepeatState))
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
private fun TrackProgress(state: BottomPanelPresenter.State, presenter: BottomPanelPresenter) {
    if (state.playbackIsPlaying == null || state.playbackProgressMs == null || state.playbackTrack == null) {
        SeekableSlider(progress = null, sliderWidth = TRACK_SLIDER_WIDTH)
    } else {
        val track = state.playbackTrack

        val progress = if (state.playbackIsPlaying) {
            remember(state) {
                flow {
                    val start = System.nanoTime()
                    while (true) {
                        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start).toInt()
                        emit(state.playbackProgressMs + elapsedMs)
                        delay(PROGRESS_SLIDER_UPDATE_DELAY_MS)
                    }
                }
            }
                .collectAsState(initial = state.playbackProgressMs, context = Dispatchers.IO)
                .value
                .coerceAtMost(track.durationMs)
        } else {
            state.playbackProgressMs
        }

        SeekableSlider(
            progress = progress.let { progress.toFloat() / track.durationMs },
            dragKey = state,
            sliderWidth = TRACK_SLIDER_WIDTH,
            leftContent = {
                Text(text = remember(progress) { formatDuration(progress) }, fontSize = Dimens.fontCaption)
            },
            rightContent = {
                Text(
                    text = remember(track.durationMs) { formatDuration(track.durationMs) },
                    fontSize = Dimens.fontCaption
                )
            },
            onSeek = { seekPercent ->
                val positionMs = (seekPercent * track.durationMs).roundToInt()
                presenter.emitAsync(BottomPanelPresenter.Event.SeekTo(positionMs = positionMs))
            }
        )
    }
}
