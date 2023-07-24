package com.dzirbel.kotify.ui.player

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository2.album.SavedAlbumRepository
import com.dzirbel.kotify.repository2.artist.SavedArtistRepository
import com.dzirbel.kotify.repository2.player.PlayerRepository
import com.dzirbel.kotify.repository2.player.SkippingState
import com.dzirbel.kotify.repository2.player.TrackPosition
import com.dzirbel.kotify.repository2.rating.Rating
import com.dzirbel.kotify.repository2.rating.TrackRatingRepository
import com.dzirbel.kotify.repository2.track.SavedTrackRepository
import com.dzirbel.kotify.repository2.util.ToggleableState
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.RefreshIcon
import com.dzirbel.kotify.ui.components.SeekableSlider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.star.StarRating
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.collectAsStateSwitchable
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.combineState
import com.dzirbel.kotify.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
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

@Composable
fun PlayerPanel() {
    val track = PlayerRepository.currentTrack.collectAsState().value
    val trackId = track?.id

    val trackRating = remember(trackId) { trackId?.let { TrackRatingRepository.ratingStateOf(id = it) } }
        ?.collectAsStateSwitchable(key = trackId)
        ?.value

    Column(Modifier.instrument().fillMaxWidth().wrapContentHeight()) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

        LocalColors.current.WithSurface {
            val layoutDirection = LocalLayoutDirection.current

            Layout(
                modifier = Modifier.surfaceBackground().padding(Dimens.space3),
                content = {
                    Column {
                        CurrentTrack(
                            track = track,
                            trackRating = trackRating,
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayerControls()
                        TrackProgress()
                    }

                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                        VolumeControls()
                        DeviceControls()
                    }
                },
                measurePolicy = { measurables, constraints ->
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
                    val centerPlaceable = center
                        .measure(constraints.copy(minWidth = centerWidth, maxWidth = centerWidth))
                    val rightPlaceable = right.measure(constraints.copy(maxWidth = rightWidth))

                    val maxHeight = maxOf(leftPlaceable.height, centerPlaceable.height, rightPlaceable.height)

                    layout(width = totalWidth, height = maxHeight) {
                        leftPlaceable.place(
                            x = Alignment.Start.align(
                                size = leftPlaceable.width,
                                space = leftWidth,
                                layoutDirection = layoutDirection,
                            ),
                            y = Alignment.CenterVertically.align(size = leftPlaceable.height, space = maxHeight),
                        )

                        centerPlaceable.place(
                            x = leftWidth + Alignment.CenterHorizontally.align(
                                size = centerPlaceable.width,
                                space = centerWidth,
                                layoutDirection = layoutDirection,
                            ),
                            y = Alignment.CenterVertically.align(size = centerPlaceable.height, space = maxHeight),
                        )

                        rightPlaceable.place(
                            x = leftWidth + centerWidth + Alignment.End.align(
                                size = rightPlaceable.width,
                                space = rightWidth,
                                layoutDirection = layoutDirection,
                            ),
                            y = Alignment.CenterVertically.align(size = rightPlaceable.height, space = maxHeight),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CurrentTrack(track: SpotifyTrack?, trackRating: Rating?) {
    Row(
        modifier = Modifier.instrument(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
    ) {
        val album = (track as? FullSpotifyTrack)?.album ?: (track as? SimplifiedSpotifyTrack)?.album

        LoadedImage(
            url = album?.images?.firstOrNull()?.url,
            size = ALBUM_ART_SIZE,
        )

        if (track != null) {
            Column(
                modifier = Modifier.sizeIn(minHeight = ALBUM_ART_SIZE),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                ) {
                    Text(track.name)

                    track.id?.let { trackId ->
                        ToggleSaveButton(repository = SavedTrackRepository, id = trackId)
                    }

                    val scope = rememberCoroutineScope { Dispatchers.IO }
                    StarRating(
                        rating = trackRating,
                        enabled = track.id != null,
                        onRate = { rating ->
                            track.id?.let { trackId ->
                                scope.launch {
                                    TrackRatingRepository.rate(id = trackId, rating = rating)
                                }
                            }
                        },
                    )
                }

                VerticalSpacer(Dimens.space3)

                Row(verticalAlignment = Alignment.Top) {
                    Text("by ", style = MaterialTheme.typography.caption)

                    Column {
                        track.artists.forEach { artist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
                            ) {
                                LinkedText(
                                    key = artist.id,
                                    style = MaterialTheme.typography.caption,
                                    onClickLink = { artistId ->
                                        pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                                    },
                                ) {
                                    link(text = artist.name, link = artist.id)
                                }

                                artist.id?.let { artistId ->
                                    ToggleSaveButton(repository = SavedArtistRepository, id = artistId)
                                }
                            }
                        }
                    }
                }

                VerticalSpacer(Dimens.space2)

                if (album != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
                    ) {
                        LinkedText(
                            key = album.id,
                            style = MaterialTheme.typography.caption,
                            onClickLink = { albumId ->
                                pageStack.mutate { to(AlbumPage(albumId = albumId)) }
                            },
                        ) {
                            text("on ")
                            link(text = album.name, link = album.id)
                        }

                        album.id?.let { albumId ->
                            ToggleSaveButton(repository = SavedAlbumRepository, id = albumId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControls() {
    val playable = PlayerRepository.playable.collectAsState().value == true

    val playing: ToggleableState<Boolean>? = PlayerRepository.playing.collectAsState().value
    val shuffling: ToggleableState<Boolean>? = PlayerRepository.shuffling.collectAsState().value
    val skipping: SkippingState = PlayerRepository.skipping.collectAsState().value
    val repeatMode: ToggleableState<SpotifyRepeatMode>? = PlayerRepository.repeatMode.collectAsState().value

    Row(
        modifier = Modifier.instrument(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        IconButton(
            enabled = playable && shuffling is ToggleableState.Set,
            onClick = {
                if (shuffling is ToggleableState.Set) {
                    PlayerRepository.setShuffle(!shuffling.value)
                }
            },
        ) {
            CachedIcon(
                name = "shuffle",
                size = Dimens.iconSmall,
                contentDescription = "Shuffle",
                tint = LocalColors.current.highlighted(highlight = shuffling?.value == true),
            )
        }

        IconButton(
            enabled = playable && skipping != SkippingState.SKIPPING_TO_PREVIOUS,
            onClick = { PlayerRepository.skipToPrevious() },
        ) {
            CachedIcon(name = "skip-previous", size = Dimens.iconSmall, contentDescription = "Previous")
        }

        IconButton(
            enabled = playable && playing is ToggleableState.Set,
            onClick = {
                if (playing is ToggleableState.Set) {
                    if (playing.value) PlayerRepository.pause() else PlayerRepository.play()
                }
            },
        ) {
            CachedIcon(
                name = if (playing?.value == true) "pause-circle-outline" else "play-circle-outline",
                contentDescription = if (playing?.value == true) "Pause" else "Play",
            )
        }

        IconButton(
            enabled = playable && skipping != SkippingState.SKIPPING_TO_NEXT,
            onClick = { PlayerRepository.skipToNext() },
        ) {
            CachedIcon(name = "skip-next", size = Dimens.iconSmall, contentDescription = "Next")
        }

        IconButton(
            enabled = playable && repeatMode is ToggleableState.Set,
            onClick = {
                if (repeatMode is ToggleableState.Set) {
                    PlayerRepository.setRepeatMode(repeatMode.value.next())
                }
            },
        ) {
            CachedIcon(
                name = if (repeatMode?.value == SpotifyRepeatMode.TRACK) "repeat-one" else "repeat",
                size = Dimens.iconSmall,
                contentDescription = "Repeat",
                tint = LocalColors.current.highlighted(
                    highlight = repeatMode?.value?.let { it != SpotifyRepeatMode.OFF } == true,
                ),
            )
        }
    }
}

@Composable
private fun TrackProgress() {
    val track = PlayerRepository.currentTrack.collectAsState().value
    val position = PlayerRepository.trackPosition.collectAsState().value

    if (track == null || position == null) {
        SeekableSlider(progress = null)
    } else {
        val progressFlow = remember(position) {
            if (position is TrackPosition.Fetched && position.playing) {
                flow {
                    val start = System.nanoTime()
                    while (true) {
                        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start).toInt()
                        emit(position.fetchedPositionMs + elapsedMs)
                        delay(PROGRESS_SLIDER_UPDATE_DELAY_MS)
                    }
                }
            } else {
                flowOf(position.currentPositionMs)
            }
        }

        val positionMs: Int = progressFlow.collectAsStateSwitchable(
            initial = { position.currentPositionMs },
            key = position,
        )
            .value
            .coerceAtMost(track.durationMs.toInt())

        SeekableSlider(
            progress = positionMs.toFloat() / track.durationMs,
            leftContent = {
                Text(
                    text = remember(positionMs) { formatDuration(positionMs.toLong()) },
                    style = MaterialTheme.typography.overline,
                )
            },
            rightContent = {
                Text(
                    text = remember(track.durationMs) { formatDuration(track.durationMs) },
                    style = MaterialTheme.typography.overline,
                )
            },
            onSeek = { seekPercent ->
                val seekPositionMs = (seekPercent * track.durationMs).roundToInt()
                PlayerRepository.seekToPosition(seekPositionMs)
            },
        )
    }
}

@Composable
private fun VolumeControls() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val volume: Int? = PlayerRepository.volume.collectAsState().value?.value

        // stores the last volume before clicking the mute button, or null if not muted
        val unmutedVolume = remember { Ref<Int>() }

        SeekableSlider(
            progress = volume?.let { it.toFloat() / 100 },
            sliderWidth = VOLUME_SLIDER_WIDTH,
            leftContent = {
                IconButton(
                    modifier = Modifier.size(Dimens.iconSmall),
                    enabled = volume != null,
                    onClick = {
                        val previousVolume = unmutedVolume.value
                        if (previousVolume == null) {
                            unmutedVolume.value = volume
                            PlayerRepository.setVolume(volumePercent = 0)
                        } else {
                            unmutedVolume.value = null
                            PlayerRepository.setVolume(volumePercent = previousVolume)
                        }
                    },
                ) {
                    CachedIcon(
                        name = if (volume == 0) "volume-off" else "volume-up",
                        contentDescription = "Volume",
                        size = Dimens.iconSmall,
                    )
                }
            },
            onSeek = { seekPercent ->
                unmutedVolume.value = null
                val volumePercent = (seekPercent * 100).roundToInt()

                // TODO maybe queue new volume requests rather than ignoring them if setting another volume
                PlayerRepository.setVolume(volumePercent)
            },
        )

        val refreshing = remember {
            listOf(
                PlayerRepository.refreshingPlayback,
                PlayerRepository.refreshingTrack,
                PlayerRepository.refreshingTrack,
            )
                .combineState { refreshingStates ->
                    // refreshing button spins when any aspect is being refreshed
                    refreshingStates.any { it }
                }
        }
            .collectAsState()
            .value

        IconButton(
            enabled = !refreshing,
            onClick = {
                PlayerRepository.refreshPlayback()
                PlayerRepository.refreshTrack()
                PlayerRepository.refreshDevices()
            },
        ) {
            RefreshIcon(refreshing = refreshing)
        }

        val errorResetCounter = remember { mutableStateOf(0) }
        val errorFlow = remember(errorResetCounter.value) {
            PlayerRepository.errors
                .runningFold(emptyList<Throwable>()) { list, throwable -> list.plus(throwable) }
        }

        val errors = errorFlow.collectAsState(initial = emptyList()).value

        if (errors.isNotEmpty()) {
            val errorsExpanded = remember { mutableStateOf(false) }
            IconButton(
                onClick = { errorsExpanded.value = !errorsExpanded.value },
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(Dimens.iconMedium),
                    tint = LocalColors.current.error,
                )

                DropdownMenu(
                    expanded = errorsExpanded.value,
                    onDismissRequest = { errorsExpanded.value = false },
                ) {
                    errors.forEach { throwable ->
                        Text(
                            modifier = Modifier.padding(Dimens.space3),
                            text = "${throwable::class.simpleName} | ${throwable.message}",
                        )

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(Dimens.divider)
                                .background(LocalColors.current.dividerColor),
                        )
                    }

                    SimpleTextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { errorResetCounter.value++ },
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceControls() {
    val devices: List<SpotifyPlaybackDevice>? = PlayerRepository.availableDevices.collectAsState().value
    val loadingDevices = PlayerRepository.refreshingDevices.collectAsState().value
    val currentDevice = PlayerRepository.currentDevice.collectAsState().value
        ?: devices?.firstOrNull()
    val dropdownEnabled = devices != null && devices.size > 1
    val dropdownExpanded = remember { mutableStateOf(false) }

    SimpleTextButton(
        enabled = dropdownEnabled,
        onClick = { dropdownExpanded.value = !dropdownExpanded.value },
    ) {
        CachedIcon(name = currentDevice.iconName, size = Dimens.iconSmall)

        HorizontalSpacer(Dimens.space3)

        val text = when {
            devices == null && loadingDevices -> "Loading devices..."
            devices == null -> "Error loading devices"
            devices.isEmpty() -> "No devices"
            currentDevice != null -> currentDevice.name
            else -> error("impossible")
        }

        Text(text)

        if (dropdownEnabled) {
            checkNotNull(devices)

            HorizontalSpacer(Dimens.space3)

            // use a custom layout in order to match width with height, which doesn't seem to be possible any other
            // way (e.g. aspectRatio() modifier)
            Layout(
                modifier = Modifier.background(color = LocalColors.current.primary, shape = CircleShape),
                content = {
                    Text(
                        text = devices.size.toString(),
                        color = LocalColors.current.textOnSurface,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.sp, // hack - ideally wouldn't be necessary
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
                            y = (size - placeable.height) / 2,
                        )
                    }
                },
            )

            DropdownMenu(
                expanded = dropdownExpanded.value,
                onDismissRequest = { dropdownExpanded.value = false },
            ) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        onClick = {
                            PlayerRepository.transferPlayback(deviceId = device.id)
                            dropdownExpanded.value = false
                        },
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

private val SpotifyPlaybackDevice?.iconName: String
    get() {
        if (this == null) return "devices-other"
        return when {
            type.equals("computer", ignoreCase = true) -> "computer"
            type.equals("smartphone", ignoreCase = true) -> "smartphone"
            else -> "devices-other"
        }
    }
