package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.PlaybackDevice
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.RefreshIcon
import com.dzirbel.kotify.ui.components.SeekableSlider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

internal class BottomPanelPresenter(scope: CoroutineScope) :
    Presenter<BottomPanelPresenter.State, BottomPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.LoadDevices(), Event.LoadPlayback(), Event.LoadTrackPlayback()),
        eventMergeStrategy = EventMergeStrategy.LATEST,
        initialState = State()
    ) {

    private val job: Job

    init {
        job = scope.launch {
            Player.playEvents.collect { playEvent ->
                emit(Event.LoadPlayback(untilIsPlayingChange = true))
                if (playEvent.contextChanged) {
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                }
            }
        }
    }

    override fun close() {
        job.cancel()
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

        // non-null when muted, saves the previous volume percent
        val savedVolume: Int? = null,

        val trackIsSaved: Boolean? = null,
        val artistsAreSaved: Map<String, Boolean>? = null,
        val albumIsSaved: Boolean? = null,

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

        fun withTrack(track: Track?): State {
            if (track == null) {
                return copy(
                    playbackTrack = null,
                    trackIsSaved = null,
                    artistsAreSaved = null,
                    albumIsSaved = null
                )
            }

            val savedArtists = LibraryCache.savedArtists
            val albumId = when (track) {
                is SimplifiedTrack -> track.album?.id
                is FullTrack -> track.album.id
                else -> null
            }
            return copy(
                playbackTrack = track,
                trackIsSaved = LibraryCache.savedTracks?.contains(track.id),
                artistsAreSaved = savedArtists?.let {
                    track.artists.mapNotNull { it.id }.associateWith { id -> savedArtists.contains(id) }
                },
                albumIsSaved = LibraryCache.savedAlbums?.contains(albumId)
            )
        }
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
        class ToggleMuteVolume(val mute: Boolean, val previousVolume: Int) : Event()
        class SeekTo(val positionMs: Int) : Event()

        class SelectDevice(val device: PlaybackDevice) : Event()

        class ToggleTrackSaved(val trackId: String, val save: Boolean) : Event()
        class ToggleAlbumSaved(val albumId: String, val save: Boolean) : Event()
        class ToggleArtistSaved(val artistId: String, val save: Boolean) : Event()
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
            events.filterIsInstance<Event.ToggleMuteVolume>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SeekTo>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.SelectDevice>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.ToggleTrackSaved>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.ToggleAlbumSaved>().transformLatest { reactTo(it) },
            events.filterIsInstance<Event.ToggleArtistSaved>().transformLatest { reactTo(it) },
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

                    else -> {
                        mutateState {
                            it
                                .withTrack(track = playback.item)
                                .copy(
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
                            it.withTrack(null).copy(loadingTrackPlayback = false)
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
                            it
                                .withTrack(track = trackPlayback.item)
                                .copy(
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
                    it.copy(savedVolume = event.volume)
                }

                Spotify.Player.setVolume(deviceId = deviceId, volumePercent = event.volume)
                emit(Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId))
            }

            is Event.ToggleMuteVolume -> {
                val deviceId: String
                val savedVolume: Int?
                mutateState {
                    deviceId = requireNotNull(it.currentDevice?.id) { "no device" }
                    savedVolume = it.savedVolume
                    it
                }

                val volume = if (event.mute) 0 else requireNotNull(savedVolume) { "no saved volume" }
                Spotify.Player.setVolume(deviceId = deviceId, volumePercent = volume)
                emit(Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId))

                mutateState {
                    it.copy(savedVolume = event.previousVolume)
                }
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

            is Event.ToggleTrackSaved -> {
                val savedTrackIds = if (event.save) {
                    SpotifyCache.Tracks.saveTrack(id = event.trackId)
                } else {
                    SpotifyCache.Tracks.unsaveTrack(id = event.trackId)
                }

                mutateState {
                    it.copy(trackIsSaved = savedTrackIds?.contains(event.trackId))
                }
            }

            is Event.ToggleAlbumSaved -> {
                val savedAlbumIds = if (event.save) {
                    SpotifyCache.Albums.saveAlbum(id = event.albumId)
                } else {
                    SpotifyCache.Albums.unsaveAlbum(id = event.albumId)
                }

                mutateState {
                    it.copy(albumIsSaved = savedAlbumIds?.contains(event.albumId))
                }
            }

            is Event.ToggleArtistSaved -> {
                val savedArtistIds = if (event.save) {
                    SpotifyCache.Artists.saveArtist(id = event.artistId)
                } else {
                    SpotifyCache.Artists.unsaveArtist(id = event.artistId)
                }

                savedArtistIds?.let {
                    mutateState {
                        it.copy(
                            artistsAreSaved = it.artistsAreSaved
                                ?.plus(event.artistId to savedArtistIds.contains(event.artistId))
                        )
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
fun BottomPanel(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { BottomPanelPresenter(scope) }

    val state = presenter.state().safeState

    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

        val layoutDirection = LocalLayoutDirection.current

        Layout(
            modifier = Modifier.background(Colors.current.surface2).padding(Dimens.space3),
            content = {
                Column {
                    CurrentTrack(
                        track = state.playbackTrack,
                        trackIsSaved = state.trackIsSaved,
                        artistsAreSaved = state.artistsAreSaved,
                        albumIsSaved = state.albumIsSaved,
                        presenter = presenter,
                        pageStack = pageStack
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerControls(state = state, presenter = presenter)

                    TrackProgress(state = state, presenter = presenter)
                }

                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
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
private fun CurrentTrack(
    track: Track?,
    trackIsSaved: Boolean?,
    artistsAreSaved: Map<String, Boolean>?,
    albumIsSaved: Boolean?,
    presenter: BottomPanelPresenter,
    pageStack: MutableState<PageStack>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        val album = (track as? FullTrack)?.album ?: (track as? SimplifiedTrack)?.album

        LoadedImage(
            url = album?.images?.firstOrNull()?.url,
            size = ALBUM_ART_SIZE
        )

        track?.let {
            Column(
                modifier = Modifier.sizeIn(minHeight = ALBUM_ART_SIZE),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space3)
                ) {
                    Text(track.name)

                    track.id?.let { trackId ->
                        ToggleSaveButton(isSaved = trackIsSaved) {
                            presenter.emitAsync(
                                BottomPanelPresenter.Event.ToggleTrackSaved(trackId = trackId, save = it)
                            )
                        }
                    }
                }

                VerticalSpacer(Dimens.space3)

                Row(verticalAlignment = Alignment.Top) {
                    Text("by ", fontSize = Dimens.fontSmall)

                    Column {
                        track.artists.forEach { artist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
                            ) {
                                LinkedText(
                                    key = artist.id,
                                    style = LocalTextStyle.current.copy(fontSize = Dimens.fontSmall),
                                    onClickLink = { artistId ->
                                        pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                                    }
                                ) {
                                    link(text = artist.name, link = artist.id)
                                }

                                artist.id?.let { artistId ->
                                    ToggleSaveButton(
                                        isSaved = artistsAreSaved?.get(artist.id),
                                        size = Dimens.iconTiny
                                    ) {
                                        presenter.emitAsync(
                                            BottomPanelPresenter.Event.ToggleArtistSaved(artistId = artistId, save = it)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalSpacer(Dimens.space2)

                if (album != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
                    ) {
                        LinkedText(
                            key = album.id,
                            style = LocalTextStyle.current.copy(fontSize = Dimens.fontSmall),
                            onClickLink = { albumId ->
                                pageStack.mutate { to(AlbumPage(albumId = albumId)) }
                            }
                        ) {
                            text("on ")
                            link(text = album.name, link = album.id)
                        }

                        album.id?.let { albumId ->
                            ToggleSaveButton(isSaved = albumIsSaved, size = Dimens.iconTiny) {
                                presenter.emitAsync(
                                    BottomPanelPresenter.Event.ToggleAlbumSaved(albumId = albumId, save = it)
                                )
                            }
                        }
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

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        IconButton(
            enabled = controlsEnabled && !state.togglingShuffle,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.ToggleShuffle(shuffle = !shuffling))
            }
        ) {
            CachedIcon(
                name = "shuffle",
                size = Dimens.iconSmall,
                contentDescription = "Shuffle",
                tint = Colors.current.highlighted(highlight = shuffling)
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.skippingPrevious,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.SkipPrevious)
            }
        ) {
            CachedIcon(name = "skip-previous", size = Dimens.iconSmall, contentDescription = "Previous")
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
            CachedIcon(
                name = if (playing) "pause-circle-outline" else "play-circle-outline",
                contentDescription = if (playing) "Pause" else "Play"
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.skippingNext,
            onClick = {
                presenter.emitAsync(BottomPanelPresenter.Event.SkipNext)
            }
        ) {
            CachedIcon(name = "skip-next", size = Dimens.iconSmall, contentDescription = "Next")
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
            CachedIcon(
                name = if (repeatState == "track") "repeat-one" else "repeat",
                size = Dimens.iconSmall,
                contentDescription = "Repeat",
                tint = Colors.current.highlighted(highlight = repeatState == "track" || repeatState == "context"),
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
        val muted = volume == 0

        SeekableSlider(
            progress = @Suppress("MagicNumber") volume?.let { it.toFloat() / 100 },
            dragKey = currentDevice,
            sliderWidth = VOLUME_SLIDER_WIDTH,
            leftContent = {
                IconButton(
                    modifier = Modifier.size(Dimens.iconSmall),
                    enabled = volume != null,
                    onClick = {
                        if (volume != null) {
                            presenter.emitAsync(
                                BottomPanelPresenter.Event.ToggleMuteVolume(mute = !muted, previousVolume = volume)
                            )
                        }
                    }
                ) {
                    CachedIcon(
                        name = if (muted) "volume-off" else "volume-up",
                        contentDescription = "Volume",
                        size = Dimens.iconSmall
                    )
                }
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
            RefreshIcon(refreshing = refreshing)
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
        CachedIcon(name = state.currentDevice.iconName, size = Dimens.iconSmall)

        HorizontalSpacer(Dimens.space3)

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

            HorizontalSpacer(Dimens.space3)

            // use a custom layout in order to match width with height, which doesn't seem to be possible any other
            // way (e.g. aspectRatio() modifier)
            Layout(
                modifier = Modifier.background(color = Colors.current.primary, shape = CircleShape),
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
                        CachedIcon(name = device.iconName, size = Dimens.iconSmall)

                        HorizontalSpacer(Dimens.space2)

                        Text(device.name)
                    }
                }
            }
        }
    }
}

private val PlaybackDevice?.iconName: String
    get() {
        if (this == null) return "devices-other"
        return when {
            type.equals("computer", ignoreCase = true) -> "computer"
            type.equals("smartphone", ignoreCase = true) -> "smartphone"
            else -> "devices-other"
        }
    }
