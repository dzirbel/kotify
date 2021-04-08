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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.PlaybackDevice
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.SeekableSlider
import com.dominiczirbel.ui.common.SimpleTextButton
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
import kotlin.math.max
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
        val playbackCurrentDevice: PlaybackDevice? = null,

        val selectedDevice: PlaybackDevice? = null,
        val devices: List<PlaybackDevice>? = null,

        val loadingPlayback: Boolean = true,
        val loadingTrackPlayback: Boolean = true,
        val loadingDevices: Boolean = true,

        val togglingShuffle: Boolean = false,
        val togglingRepeat: Boolean = false,
        val togglingPlayback: Boolean = false,
        val skippingPrevious: Boolean = false,
        val skippingNext: Boolean = false
    ) {
        val currentDevice: PlaybackDevice?
            get() = selectedDevice ?: playbackCurrentDevice ?: devices?.firstOrNull()
    }

    sealed class Event {
        object LoadDevices : Event()
        object LoadPlayback : Event()
        class LoadTrackPlayback(val untilTrackChange: Boolean = false, val retries: Int = 5) : Event()

        object Play : Event()
        object Pause : Event()
        object SkipNext : Event()
        object SkipPrevious : Event()
        class ToggleShuffle(val shuffle: Boolean) : Event()
        class SetRepeat(val repeatState: String) : Event()
        class SetVolume(val volume: Int) : Event()
        class SeekTo(val positionMs: Int) : Event()

        class SelectDevice(val device: PlaybackDevice) : Event()
    }

    override fun reactTo(events: Flow<Event>): Flow<Event> {
        return merge(
            events.filterIsInstance<Event.LoadDevices>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.LoadPlayback>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.LoadTrackPlayback>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.Play>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.Pause>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SkipNext>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SkipPrevious>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.ToggleShuffle>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SetRepeat>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SetVolume>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SeekTo>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SelectDevice>().transformLatest { reactTo(it) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.LoadDevices -> {
                mutateState { it.copy(loadingDevices = true) }

                val devices = try {
                    Spotify.Player.getAvailableDevices()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingDevices = false) }
                    throw ex
                }

                Player.playable.value = devices.isNotEmpty()

                mutateState {
                    it.copy(devices = devices, loadingDevices = false)
                }
            }

            Event.LoadPlayback -> {
                mutateState { it.copy(loadingPlayback = true) }

                val playback = try {
                    // TODO sometimes returns before playback state has changed when pausing/resuming
                    Spotify.Player.getCurrentPlayback()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingPlayback = false) }
                    throw ex
                }

                mutateState {
                    it.copy(
                        playbackTrack = playback?.item ?: it.playbackTrack,
                        playbackProgressMs = playback?.progressMs ?: it.playbackProgressMs,
                        playbackIsPlaying = playback?.isPlaying ?: it.playbackIsPlaying,
                        playbackShuffleState = playback?.shuffleState ?: it.playbackShuffleState,
                        playbackRepeatState = playback?.repeatState ?: it.playbackRepeatState,
                        playbackCurrentDevice = playback?.device,
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

                val trackPlayback = try {
                    Spotify.Player.getCurrentlyPlayingTrack()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingTrackPlayback = false) }
                    throw ex
                }

                when {
                    trackPlayback == null ->
                        mutateState {
                            it.copy(loadingTrackPlayback = false, playbackTrack = null)
                        }

                    trackPlayback.item == null -> {
                        if (event.retries > 0) {
                            // try again until we get a valid track
                            delay(REFRESH_BUFFER_MS)

                            emit(
                                Event.LoadTrackPlayback(
                                    untilTrackChange = event.untilTrackChange,
                                    retries = event.retries - 1
                                )
                            )
                        } else {
                            mutateState { it.copy(loadingTrackPlayback = false) }
                        }
                    }

                    event.untilTrackChange && trackPlayback.item == currentTrack -> {
                        if (event.retries > 0) {
                            // try again until the track changes
                            delay(REFRESH_BUFFER_MS)

                            emit(Event.LoadTrackPlayback(untilTrackChange = true, retries = event.retries - 1))
                        } else {
                            mutateState { it.copy(loadingTrackPlayback = false) }
                        }
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
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(togglingPlayback = true)
                }

                try {
                    Spotify.Player.startPlayback(deviceId = deviceId)
                    emit(Event.LoadPlayback)
                    emit(Event.LoadTrackPlayback())
                } finally {
                    mutateState { it.copy(togglingPlayback = false) }
                }
            }

            Event.Pause -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(togglingPlayback = true)
                }

                try {
                    Spotify.Player.pausePlayback(deviceId = deviceId)
                    emit(Event.LoadPlayback)
                    emit(Event.LoadTrackPlayback())
                } finally {
                    mutateState { it.copy(togglingPlayback = false) }
                }
            }

            Event.SkipNext -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(skippingNext = true)
                }

                try {
                    Spotify.Player.skipToNext(deviceId = deviceId)
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                } finally {
                    mutateState { it.copy(skippingNext = false) }
                }
            }

            Event.SkipPrevious -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(skippingPrevious = true)
                }

                try {
                    Spotify.Player.skipToPrevious(deviceId = deviceId)
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                } finally {
                    mutateState { it.copy(skippingPrevious = false) }
                }
            }

            is Event.ToggleShuffle -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(togglingShuffle = true)
                }

                try {
                    Spotify.Player.toggleShuffle(deviceId = deviceId, state = event.shuffle)
                    emit(Event.LoadPlayback)
                } finally {
                    mutateState { it.copy(togglingShuffle = false) }
                }
            }

            is Event.SetRepeat -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it.copy(togglingRepeat = true)
                }

                try {
                    Spotify.Player.setRepeatMode(deviceId = deviceId, state = event.repeatState)
                    emit(Event.LoadPlayback)
                } finally {
                    mutateState { it.copy(togglingRepeat = false) }
                }
            }

            is Event.SetVolume -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it
                }

                Spotify.Player.setVolume(deviceId = deviceId, volumePercent = event.volume)
                emit(Event.LoadDevices)
            }

            is Event.SeekTo -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it
                }

                Spotify.Player.seekToPosition(deviceId = deviceId, positionMs = event.positionMs)
                emit(Event.LoadPlayback)
            }

            is Event.SelectDevice -> {
                val previousSelectedDevice: PlaybackDevice?
                mutateState {
                    previousSelectedDevice = it.selectedDevice
                    it.copy(selectedDevice = event.device)
                }

                try {
                    Spotify.Player.transferPlayback(deviceIds = listOf(event.device.id))
                } catch (ex: Throwable) {
                    mutateState { it.copy(selectedDevice = previousSelectedDevice) }
                    throw ex
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

    val state = presenter.state().safeState

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

            PlaybackControls(state = state, presenter = presenter)
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

    val playing = state.playbackIsPlaying == true
    val shuffling = state.playbackShuffleState == true
    val repeatState = state.playbackRepeatState

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = controlsEnabled && !state.togglingShuffle,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.ToggleShuffle(shuffle = !shuffling))
            }
        ) {
            Icon(
                painter = svgResource("shuffle.svg"),
                modifier = Modifier.size(Dimens.iconSmall),
                tint = if (shuffling) {
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
                    if (playing) {
                        BottomPanelPresenter.Event.Pause
                    } else {
                        BottomPanelPresenter.Event.Play
                    }
                )
            }
        ) {
            Icon(
                painter = svgResource(if (playing) "pause-circle-outline.svg" else "play-circle-outline.svg"),
                modifier = Modifier.size(Dimens.iconMedium),
                contentDescription = if (playing) "Pause" else "Play"
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

@Composable
private fun PlaybackControls(state: BottomPanelPresenter.State, presenter: BottomPanelPresenter) {
    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
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

            val refreshing = state.loadingDevices || state.loadingPlayback || state.loadingTrackPlayback
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

            val errors = presenter.errors
            if (errors.isNotEmpty()) {
                val errorsExpanded = remember { mutableStateOf(false) }
                IconButton(
                    onClick = { errorsExpanded.value = !errorsExpanded.value }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(Dimens.iconMedium),
                        tint = Colors.current.error
                    )

                    DropdownMenu(
                        expanded = errorsExpanded.value,
                        onDismissRequest = { errorsExpanded.value = false }
                    ) {
                        errors.forEach { throwable ->
                            Text(
                                modifier = Modifier.padding(Dimens.space3),
                                text = "${throwable::class.simpleName} | ${throwable.message}"
                            )

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.divider)
                                    .background(Colors.current.dividerColor)
                            )
                        }

                        SimpleTextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { presenter.errors = emptyList() }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        val devices = state.devices
        val currentDevice = state.currentDevice
        val dropdownEnabled = devices != null && devices.size > 1
        val dropdownExpanded = remember { mutableStateOf(false) }

        SimpleTextButton(
            enabled = dropdownEnabled,
            onClick = { dropdownExpanded.value = !dropdownExpanded.value }
        ) {
            Icon(
                painter = svgResource(state.currentDevice.iconName),
                modifier = Modifier.size(Dimens.iconSmall),
                contentDescription = null
            )

            Spacer(Modifier.width(Dimens.space3))

            val text = when {
                devices == null && state.loadingDevices -> "Loading devices..."
                devices == null -> "Error loading devices"
                devices.isEmpty() -> "No devices"
                currentDevice != null -> currentDevice.name
                else -> error("impossible")
            }

            Text(text)

            if (dropdownEnabled) {
                devices!!

                Spacer(Modifier.width(Dimens.space3))

                // use a custom layout in order to match width with height, which doesn't seem to be possible any other
                // way (e.g. aspectRatio() modifier)
                Layout(
                    modifier = Modifier.background(color = MaterialTheme.colors.primary, shape = CircleShape),
                    content = {
                        Text(
                            text = devices.size.toString(),
                            color = Colors.current.textOnSurface,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.sp // hack - ideally wouldn't be necessary
                        )
                    },
                    measurePolicy = { measurables, constraints ->
                        check(measurables.size == 1)

                        val placeable = measurables[0].measure(constraints)
                        val size = max(placeable.width, placeable.height)

                        layout(width = size, height = size) {
                            // center vertically and horizontally
                            placeable.place(
                                x = (size - placeable.width) / 2,
                                y = (size - placeable.height) / 2
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = dropdownExpanded.value,
                    onDismissRequest = { dropdownExpanded.value = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(BottomPanelPresenter.Event.SelectDevice(device = device))
                                dropdownExpanded.value = false
                            }
                        ) {
                            Icon(
                                painter = svgResource(device.iconName),
                                modifier = Modifier.size(Dimens.iconSmall),
                                contentDescription = null
                            )

                            Spacer(Modifier.width(Dimens.space2))

                            Text(device.name)
                        }
                    }
                }
            }
        }
    }
}

private val PlaybackDevice?.iconName: String
    get() {
        if (this == null) return "devices-other.svg"
        return when {
            type.equals("computer", ignoreCase = true) -> "computer.svg"
            type.equals("smartphone", ignoreCase = true) -> "smartphone.svg"
            else -> "devices-other.svg"
        }
    }
