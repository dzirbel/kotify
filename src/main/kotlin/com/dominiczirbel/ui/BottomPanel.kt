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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.PlaybackDevice
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.LinkedText
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.common.SeekableSlider
import com.dominiczirbel.ui.common.SimpleTextButton
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import com.dominiczirbel.util.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

private val ALBUM_ART_SIZE = 75.dp
private val MIN_TRACK_PLAYBACK_WIDTH = ALBUM_ART_SIZE + 75.dp
private val MAX_TRACK_PROGRESS_WIDTH = 1000.dp
private val VOLUME_SLIDER_WIDTH = 100.dp

private const val SIDE_CONTROLS_WEIGHT = 0.25f
private const val CENTER_CONTROLS_WEIGHT = 0.5f

// time in milliseconds between updating the track progress slider
private const val PROGRESS_SLIDER_UPDATE_DELAY_MS = 50L

private class BottomPanelPresenter(scope: CoroutineScope) :
    Presenter<BottomPanelPresenter.State, BottomPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.LoadDevices(), Event.LoadPlayback(), Event.LoadTrackPlayback()),
        eventMergeStrategy = EventMergeStrategy.LATEST,
        initialState = State()
    ) {

    init {
        scope.launch {
            Player.playEvents.collect {
                emit(Event.LoadPlayback())
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
        data class LoadDevices(
            val untilVolumeChange: Boolean = false,
            val untilVolumeChangeDeviceId: String? = null,
            val retries: Int = 5
        ) : Event()

        data class LoadPlayback(
            val untilIsPlayingChange: Boolean = false,
            val untilShuffleStateChange: Boolean = false,
            val untilRepeatStateChange: Boolean = false,
            val retries: Int = 5
        ) : Event()

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
            is Event.LoadDevices -> {
                val previousVolume: Int?
                mutateState {
                    previousVolume = if (event.untilVolumeChangeDeviceId != null) {
                        it.devices?.find { device -> device.id == event.untilVolumeChangeDeviceId }?.volumePercent
                    } else {
                        null
                    }

                    it.copy(loadingDevices = true)
                }

                val devices = try {
                    Spotify.Player.getAvailableDevices()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingDevices = false) }
                    throw ex
                }

                val expectedChangeDevice = if (event.untilVolumeChangeDeviceId != null) {
                    devices.find { it.id == event.untilVolumeChangeDeviceId }
                } else {
                    null
                }

                @Suppress("ComplexCondition")
                if (event.untilVolumeChange &&
                    event.retries > 0 &&
                    expectedChangeDevice != null &&
                    expectedChangeDevice.volumePercent == previousVolume
                ) {
                    emit(event.copy(retries = event.retries - 1))
                } else {
                    val currentDevice: PlaybackDevice?
                    mutateState {
                        it.copy(devices = devices, loadingDevices = false)
                            .also { newState -> currentDevice = newState.currentDevice }
                    }
                    Player.currentDevice.value = currentDevice
                }
            }

            is Event.LoadPlayback -> {
                val previousIsPlaying: Boolean?
                val previousRepeatState: String?
                val previousShuffleState: Boolean?
                mutateState {
                    previousIsPlaying = it.playbackIsPlaying
                    previousRepeatState = it.playbackRepeatState
                    previousShuffleState = it.playbackShuffleState

                    it.copy(loadingPlayback = true)
                }

                val playback = try {
                    Spotify.Player.getCurrentPlayback()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingPlayback = false) }
                    throw ex
                }

                playback?.let {
                    Player.isPlaying.value = it.isPlaying
                    Player.playbackContext.value = it.context
                }
                playback?.item?.let { track ->
                    Player.currentTrack.value = track
                    SpotifyCache.put(track)
                }

                when {
                    playback == null -> mutateState { it.copy(loadingPlayback = false) }

                    event.untilIsPlayingChange && event.retries > 0 && playback.isPlaying == previousIsPlaying -> {
                        // try again until the playing state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    event.untilRepeatStateChange && event.retries > 0 &&
                        playback.repeatState == previousRepeatState -> {
                        // try again until the repeat state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    event.untilShuffleStateChange && event.retries > 0 &&
                        playback.shuffleState == previousShuffleState -> {
                        // try again until the shuffle state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    else -> mutateState {
                        it.copy(
                            playbackTrack = playback.item ?: it.playbackTrack,
                            playbackProgressMs = playback.progressMs,
                            playbackIsPlaying = playback.isPlaying,
                            playbackShuffleState = playback.shuffleState,
                            playbackRepeatState = playback.repeatState,
                            playbackCurrentDevice = playback.device,
                            loadingPlayback = false
                        )
                    }
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

                trackPlayback?.let {
                    Player.isPlaying.value = it.isPlaying
                    Player.playbackContext.value = it.context
                }
                trackPlayback?.item?.let { track ->
                    Player.currentTrack.value = track
                    SpotifyCache.put(track)
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
                    emit(Event.LoadPlayback(untilIsPlayingChange = true))
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
                    emit(Event.LoadPlayback(untilIsPlayingChange = true))
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
                    emit(Event.LoadPlayback(untilShuffleStateChange = true))
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
                    emit(Event.LoadPlayback(untilRepeatStateChange = true))
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
                emit(Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId))
            }

            is Event.SeekTo -> {
                val deviceId: String
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    it
                }

                Spotify.Player.seekToPosition(deviceId = deviceId, positionMs = event.positionMs)

                // hack: wait a bit to ensure we have a load after the seek has come into effect, otherwise sometimes
                // the next playback load still has the old position
                delay(REFRESH_BUFFER_MS)
                emit(Event.LoadPlayback())
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
fun BottomPanel(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope()
    val presenter = remember { BottomPanelPresenter(scope) }

    val state = presenter.state().safeState

    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

        val layoutDirection = LocalLayoutDirection.current

        Layout(
            modifier = Modifier.background(Colors.current.surface2).padding(Dimens.space3),
            content = {
                Column {
                    CurrentTrack(track = state.playbackTrack, pageStack = pageStack)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerControls(state = state, presenter = presenter)

                    TrackProgress(state = state, presenter = presenter)
                }

                Column(verticalArrangement = Arrangement.Center) {
                    VolumeControls(state = state, presenter = presenter)

                    DeviceControls(state = state, presenter = presenter)
                }
            },
            measurePolicy = @Suppress("UnnecessaryParentheses") { measurables, constraints ->
                @Suppress("MagicNumber")
                check(measurables.size == 3)

                val totalWidth = constraints.maxWidth

                val left = measurables[0]
                val center = measurables[1]
                val right = measurables[2]

                // base widths according to the weights
                val sideWidthBase = (totalWidth * SIDE_CONTROLS_WEIGHT).roundToInt()
                val centerWidthBase = (totalWidth * CENTER_CONTROLS_WEIGHT).roundToInt()

                // width constraint constants
                val maxCenterWidth = MAX_TRACK_PROGRESS_WIDTH.roundToPx()
                val minLeftWidth = MIN_TRACK_PLAYBACK_WIDTH.roundToPx()

                // allocate any extra space due to the maxCenterWidth to the sides
                val sideExtra = ((totalWidth - maxCenterWidth - (sideWidthBase * 2)) / 2)
                    .coerceAtLeast(0)

                // left width calculated first with the highest priority
                val leftWidth = (sideWidthBase + sideExtra).coerceAtLeast(minLeftWidth)

                // right width calculated next with the second priority, giving space to left if necessary
                val rightWidth = (sideWidthBase + sideExtra).coerceAtMost(totalWidth - leftWidth).coerceAtLeast(0)

                // center width calculated last, gives any possible space to left/right
                val centerWidth = centerWidthBase
                    .coerceAtMost(maxCenterWidth)
                    .coerceAtMost(totalWidth - leftWidth - rightWidth)
                    .coerceAtLeast(0)

                val leftPlaceable = left.measure(constraints.copy(minWidth = leftWidth, maxWidth = leftWidth))
                val centerPlaceable = center.measure(constraints.copy(minWidth = centerWidth, maxWidth = centerWidth))
                val rightPlaceable = right.measure(constraints.copy(maxWidth = rightWidth))

                val maxHeight = maxOf(leftPlaceable.height, centerPlaceable.height, rightPlaceable.height)

                layout(width = totalWidth, height = maxHeight) {
                    leftPlaceable.place(
                        x = Alignment.Start.align(
                            size = leftPlaceable.width,
                            space = leftWidth,
                            layoutDirection = layoutDirection
                        ),
                        y = Alignment.CenterVertically.align(size = leftPlaceable.height, space = maxHeight)
                    )

                    centerPlaceable.place(
                        x = leftWidth + Alignment.CenterHorizontally.align(
                            size = centerPlaceable.width,
                            space = centerWidth,
                            layoutDirection = layoutDirection
                        ),
                        y = Alignment.CenterVertically.align(size = centerPlaceable.height, space = maxHeight)
                    )

                    rightPlaceable.place(
                        x = leftWidth + centerWidth + Alignment.End.align(
                            size = rightPlaceable.width,
                            space = rightWidth,
                            layoutDirection = layoutDirection
                        ),
                        y = Alignment.CenterVertically.align(size = rightPlaceable.height, space = maxHeight)
                    )
                }
            }
        )
    }
}

@Composable
private fun CurrentTrack(track: Track?, pageStack: MutableState<PageStack>) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        val album = (track as? FullTrack)?.album ?: (track as? SimplifiedTrack)?.album

        LoadedImage(
            url = album?.images?.firstOrNull()?.url,
            size = ALBUM_ART_SIZE,
            scope = rememberCoroutineScope()
        )

        track?.let {
            Column(
                modifier = Modifier.sizeIn(minHeight = ALBUM_ART_SIZE),
                verticalArrangement = Arrangement.Center
            ) {
                Text(track.name)

                Spacer(Modifier.height(Dimens.space3))

                LinkedText(
                    key = track.artists,
                    style = LocalTextStyle.current.copy(fontSize = Dimens.fontSmall),
                    onClickLink = { artistId ->
                        pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                    }
                ) {
                    track.artists.forEachIndexed { index, artist ->
                        link(text = artist.name, link = artist.id)

                        if (index != track.artists.lastIndex) {
                            text(", ")
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.space2))

                if (album != null) {
                    LinkedText(
                        key = album,
                        style = LocalTextStyle.current.copy(fontSize = Dimens.fontSmall),
                        onClickLink = { albumId ->
                            pageStack.mutate { to(AlbumPage(albumId = albumId)) }
                        }
                    ) {
                        link(text = album.name, link = album.id)
                    }
                }
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
        SeekableSlider(progress = null)
    } else {
        val track = state.playbackTrack

        // save the last manual seek position, which is used when playback is loading to avoid jumps
        val seekProgress = remember(state.playbackProgressMs, state.playbackIsPlaying) { mutableStateOf<Int?>(null) }

        val currentSeekProgress = seekProgress.value
        val progress = if ((state.loadingPlayback || state.togglingPlayback) && currentSeekProgress != null) {
            currentSeekProgress.toLong()
        } else {
            remember(state.playbackProgressMs, state.playbackIsPlaying) {
                if (state.playbackIsPlaying) {
                    flow {
                        val start = System.nanoTime()
                        while (true) {
                            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start).toInt()
                            emit(state.playbackProgressMs + elapsedMs)
                            delay(PROGRESS_SLIDER_UPDATE_DELAY_MS)
                        }
                    }
                } else {
                    flowOf(state.playbackProgressMs)
                }
            }
                .collectAsState(initial = state.playbackProgressMs, context = Dispatchers.Default)
                .value
                .coerceAtMost(track.durationMs)
        }

        SeekableSlider(
            progress = progress.toFloat() / track.durationMs,
            dragKey = state,
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
                seekProgress.value = positionMs
                presenter.emitAsync(BottomPanelPresenter.Event.SeekTo(positionMs = positionMs))
            }
        )
    }
}

@Composable
private fun VolumeControls(state: BottomPanelPresenter.State, presenter: BottomPanelPresenter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val devices = state.devices
        val currentDevice = devices?.firstOrNull()

        val seekVolume = remember(currentDevice?.volumePercent) { mutableStateOf<Int?>(null) }

        val currentSeekVolume = seekVolume.value
        val volume = if (state.loadingPlayback && currentSeekVolume != null) {
            currentSeekVolume
        } else {
            currentDevice?.volumePercent
        }

        SeekableSlider(
            progress = @Suppress("MagicNumber") volume?.let { it.toFloat() / 100 },
            dragKey = currentDevice,
            sliderWidth = VOLUME_SLIDER_WIDTH,
            leftContent = {
                Icon(
                    painter = svgResource("volume-up.svg"),
                    contentDescription = "Volume"
                )
            },
            onSeek = { seekPercent ->
                val volumeInt = @Suppress("MagicNumber") (seekPercent * 100).roundToInt()
                seekVolume.value = volumeInt
                presenter.emitAsync(BottomPanelPresenter.Event.SetVolume(volumeInt))
            }
        )

        val refreshing = state.loadingDevices || state.loadingPlayback || state.loadingTrackPlayback
        IconButton(
            enabled = !refreshing,
            onClick = {
                presenter.emitAsync(
                    BottomPanelPresenter.Event.LoadDevices(),
                    BottomPanelPresenter.Event.LoadPlayback(),
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
}

@Composable
private fun DeviceControls(state: BottomPanelPresenter.State, presenter: BottomPanelPresenter) {
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

private val PlaybackDevice?.iconName: String
    get() {
        if (this == null) return "devices-other.svg"
        return when {
            type.equals("computer", ignoreCase = true) -> "computer.svg"
            type.equals("smartphone", ignoreCase = true) -> "smartphone.svg"
            else -> "devices-other.svg"
        }
    }
