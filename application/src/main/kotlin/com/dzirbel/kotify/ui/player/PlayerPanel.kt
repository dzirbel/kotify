package com.dzirbel.kotify.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzirbel.kotify.network.FullSpotifyTrackOrEpisode
import com.dzirbel.kotify.network.model.FullSpotifyEpisode
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.repository.player.SkippingState
import com.dzirbel.kotify.repository.player.TrackPosition
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.LocalSavedAlbumRepository
import com.dzirbel.kotify.ui.LocalSavedArtistRepository
import com.dzirbel.kotify.ui.LocalSavedTrackRepository
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.SeekableSlider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.star.StarRating
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.coroutines.combineState
import com.dzirbel.kotify.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlin.math.max
import kotlin.math.roundToInt

private val ALBUM_ART_SIZE = 92.dp
private val MIN_TRACK_PLAYBACK_WIDTH = ALBUM_ART_SIZE + 75.dp
private val MAX_TRACK_PROGRESS_WIDTH = 1000.dp
private val VOLUME_SLIDER_WIDTH = 100.dp

private const val SIDE_CONTROLS_WEIGHT = 0.25f
private const val CENTER_CONTROLS_WEIGHT = 0.5f

// time in milliseconds between updating the track progress slider
private const val PROGRESS_SLIDER_UPDATE_DELAY_MS = 50L

@Composable
fun PlayerPanel() {
    val item = LocalPlayer.current.currentItem.collectAsState().value
    val itemId = item?.id

    val ratingRepository = LocalRatingRepository.current
    val trackRating = remember(itemId) { itemId?.let { ratingRepository.ratingStateOf(id = it) } }
        ?.collectAsState()
        ?.value

    Surface(elevation = Dimens.panelElevationLarge) {
        val layoutDirection = LocalLayoutDirection.current

        Layout(
            modifier = Modifier.padding(Dimens.space3),
            content = {
                Column {
                    CurrentTrack(item = item, trackRating = trackRating)
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

// TODO use view models
@Composable
private fun CurrentTrack(item: FullSpotifyTrackOrEpisode?, trackRating: Rating?) {
    Row(
        modifier = Modifier.instrument(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        val album = (item as? FullSpotifyTrack)?.album
        val show = (item as? FullSpotifyEpisode)?.show

        LoadedImage(
            url = album?.images?.firstOrNull()?.url,
            size = ALBUM_ART_SIZE,
        )

        if (item != null) {
            Column(
                modifier = Modifier.sizeIn(minHeight = ALBUM_ART_SIZE),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
                ) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body1) {
                        Text(item.name.orEmpty())

                        if (item is FullSpotifyTrack) {
                            ToggleSaveButton(
                                repository = LocalSavedTrackRepository.current,
                                id = item.id,
                                size = Dimens.fontDp,
                            )

                            val ratingRepository = LocalRatingRepository.current
                            StarRating(
                                rating = trackRating,
                                onRate = { rating -> ratingRepository.rate(id = item.id, rating = rating) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.space2))

                if (item is FullSpotifyTrack) {
                    Row(verticalAlignment = Alignment.Top) {
                        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body2) {
                            Text("by ")

                            Column {
                                item.artists.forEach { artist ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        LinkedText(
                                            key = artist.id,
                                            onClickLink = { artistId ->
                                                pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                                            },
                                        ) {
                                            link(text = artist.name, link = artist.id)
                                        }

                                        artist.id?.let { artistId ->
                                            ToggleSaveButton(
                                                repository = LocalSavedArtistRepository.current,
                                                id = artistId,
                                                size = Dimens.fontDp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body2) {
                    if (album != null) {
                        Spacer(Modifier.height(Dimens.space1))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LinkedText(
                                key = album.id,
                                onClickLink = { albumId ->
                                    pageStack.mutate { to(AlbumPage(albumId = albumId)) }
                                },
                            ) {
                                text("on ")
                                link(text = album.name, link = album.id)
                            }

                            album.id?.let { albumId ->
                                ToggleSaveButton(
                                    repository = LocalSavedAlbumRepository.current,
                                    id = albumId,
                                    size = Dimens.fontDp,
                                )
                            }
                        }
                    }

                    if (show != null) {
                        Spacer(Modifier.height(Dimens.space1))
                        Text("on ${show.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControls() {
    val player = LocalPlayer.current

    val playable = player.playable.collectAsState().value == true

    val playing: ToggleableState<Boolean>? = player.playing.collectAsState().value
    val shuffling: ToggleableState<Boolean>? = player.shuffling.collectAsState().value
    val skipping: SkippingState = player.skipping.collectAsState().value
    val repeatMode: ToggleableState<SpotifyRepeatMode>? = player.repeatMode.collectAsState().value

    Row(
        modifier = Modifier.instrument(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        IconButton(
            enabled = playable && shuffling is ToggleableState.Set,
            onClick = {
                if (shuffling is ToggleableState.Set) {
                    player.setShuffle(!shuffling.value)
                }
            },
        ) {
            CachedIcon(
                name = "shuffle",
                size = Dimens.iconSmall,
                contentDescription = "Shuffle",
                tint = KotifyColors.highlighted(highlight = shuffling?.value == true),
            )
        }

        IconButton(
            enabled = playable && skipping != SkippingState.SKIPPING_TO_PREVIOUS,
            onClick = { player.skipToPrevious() },
        ) {
            CachedIcon(name = "skip-previous", size = Dimens.iconSmall, contentDescription = "Previous")
        }

        IconButton(
            enabled = playable && playing is ToggleableState.Set,
            onClick = {
                if (playing is ToggleableState.Set) {
                    if (playing.value) player.pause() else player.play()
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
            onClick = { player.skipToNext() },
        ) {
            CachedIcon(name = "skip-next", size = Dimens.iconSmall, contentDescription = "Next")
        }

        IconButton(
            enabled = playable && repeatMode is ToggleableState.Set,
            onClick = {
                if (repeatMode is ToggleableState.Set) {
                    player.setRepeatMode(repeatMode.value.next())
                }
            },
        ) {
            CachedIcon(
                name = if (repeatMode?.value == SpotifyRepeatMode.TRACK) "repeat-one" else "repeat",
                size = Dimens.iconSmall,
                contentDescription = "Repeat",
                tint = KotifyColors.highlighted(
                    highlight = repeatMode?.value?.let { it != SpotifyRepeatMode.OFF } == true,
                ),
            )
        }
    }
}

@Composable
private fun TrackProgress() {
    val player = LocalPlayer.current
    val track = player.currentItem.collectAsState().value
    val position = player.trackPosition.collectAsState().value

    if (track == null || position == null) {
        SeekableSlider(progress = { null })
    } else {
        val progressFlow = remember(position) {
            if (position is TrackPosition.Fetched && position.playing == true) {
                flow {
                    while (true) {
                        emit(position.currentPositionMs.coerceAtMost(track.durationMs.toInt()))
                        delay(PROGRESS_SLIDER_UPDATE_DELAY_MS)
                    }
                }
            } else {
                flowOf(position.currentPositionMs)
            }
        }

        val positionState = progressFlow.collectAsState(initial = position.currentPositionMs)

        SeekableSlider(
            progress = { positionState.value.toFloat() / track.durationMs },
            leftLabel = {
                Text(
                    text = positionState.derived { positionMs -> formatDuration(positionMs.toLong()) }.value,
                    style = MaterialTheme.typography.overline,
                )
            },
            rightLabel = {
                Text(
                    text = remember(track.durationMs) { formatDuration(track.durationMs) },
                    style = MaterialTheme.typography.overline,
                )
            },
            onSeek = { seekPercent ->
                val seekPositionMs = (seekPercent * track.durationMs).roundToInt()
                player.seekToPosition(seekPositionMs)
            },
        )
    }
}

@Composable
private fun VolumeControls() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val player = LocalPlayer.current
        val volumeState = player.volume.collectAsState()

        // stores the last volume before clicking the mute button, or null if not muted
        val unmutedVolume = remember { Ref<Int>() }

        SeekableSlider(
            progress = { volumeState.value?.value?.let { it.toFloat() / 100 } },
            sliderWidth = VOLUME_SLIDER_WIDTH,
            leftLabel = {
                IconButton(
                    modifier = Modifier.size(Dimens.iconSmall),
                    enabled = volumeState.value != null,
                    onClick = {
                        val previousVolume = unmutedVolume.value
                        if (previousVolume == null) {
                            unmutedVolume.value = volumeState.value?.value
                            player.setVolume(volumePercent = 0)
                        } else {
                            unmutedVolume.value = null
                            player.setVolume(volumePercent = previousVolume)
                        }
                    },
                ) {
                    CachedIcon(
                        name = if (volumeState.value?.value == 0) "volume-off" else "volume-up",
                        contentDescription = "Volume",
                        size = Dimens.iconSmall,
                    )
                }
            },
            onSeek = { seekPercent ->
                unmutedVolume.value = null
                val volumePercent = (seekPercent * 100).roundToInt()

                // TODO maybe queue new volume requests rather than ignoring them if setting another volume
                player.setVolume(volumePercent)
            },
        )

        val refreshing = remember {
            listOf(player.refreshingPlayback, player.refreshingTrack, player.refreshingTrack)
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
                player.refreshPlayback()
                player.refreshTrack()
                player.refreshDevices()
            },
        ) {
            if (refreshing) {
                CircularProgressIndicator(Modifier.size(Dimens.iconMedium))
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(Dimens.iconMedium),
                )
            }
        }

        val errorResetCounter = remember { mutableStateOf(0) }
        val errorFlow = remember(errorResetCounter.value) {
            player.errors.runningFold(emptyList<Throwable>()) { list, throwable -> list.plus(throwable) }
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
                    tint = MaterialTheme.colors.error,
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

                        Divider()
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
    val player = LocalPlayer.current

    val devices: List<SpotifyPlaybackDevice>? = player.availableDevices.collectAsState().value
    val loadingDevices = player.refreshingDevices.collectAsState().value
    val currentDevice = player.currentDevice.collectAsState().value
        ?: devices?.firstOrNull()
    val dropdownEnabled = devices != null && devices.size > 1
    val dropdownExpanded = remember { mutableStateOf(false) }

    SimpleTextButton(
        enabled = dropdownEnabled,
        onClick = { dropdownExpanded.value = !dropdownExpanded.value },
    ) {
        CachedIcon(name = currentDevice.iconName, size = Dimens.iconSmall)

        Spacer(Modifier.width(Dimens.space3))

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

            Spacer(Modifier.width(Dimens.space3))

            // use a custom layout in order to match width with height, which doesn't seem to be possible any other
            // way (e.g. aspectRatio() modifier)
            Layout(
                modifier = Modifier.background(color = MaterialTheme.colors.primary, shape = CircleShape),
                content = {
                    Text(
                        text = devices.size.toString(),
                        color = MaterialTheme.colors.onPrimary,
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
                            player.transferPlayback(deviceId = device.id)
                            dropdownExpanded.value = false
                        },
                    ) {
                        CachedIcon(name = device.iconName, size = Dimens.iconSmall)

                        Spacer(Modifier.width(Dimens.space2))

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
