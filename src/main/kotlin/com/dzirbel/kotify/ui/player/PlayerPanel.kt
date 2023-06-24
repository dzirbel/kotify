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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository.Rating
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
import com.dzirbel.kotify.util.formatDuration
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { PlayerPanelPresenter(scope) }

    val state = presenter.state().safeState

    Column(Modifier.instrument().fillMaxWidth().wrapContentHeight()) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

        LocalColors.current.WithSurface {
            val layoutDirection = LocalLayoutDirection.current

            Layout(
                modifier = Modifier.surfaceBackground().padding(Dimens.space3),
                content = {
                    Column {
                        CurrentTrack(
                            track = state.playbackTrack,
                            trackIsSaved = state.trackSavedState
                                ?.collectAsStateSwitchable(key = state.playbackTrack?.id)
                                ?.value,
                            trackRating = state.trackRatingState?.value,
                            artistsAreSaved = state.artistSavedStates
                                ?.mapValues { entry ->
                                    entry.value.collectAsStateSwitchable(key = entry.key).value
                                }
                                ?.toPersistentMap(),
                            albumIsSaved = state.albumSavedState
                                ?.collectAsStateSwitchable(key = state.playbackTrack?.album?.id)
                                ?.value,
                            presenter = presenter,
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
private fun CurrentTrack(
    track: SpotifyTrack?,
    trackIsSaved: Boolean?,
    trackRating: Rating?,
    artistsAreSaved: ImmutableMap<String, Boolean?>?,
    albumIsSaved: Boolean?,
    presenter: PlayerPanelPresenter,
) {
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
                        ToggleSaveButton(isSaved = trackIsSaved) {
                            presenter.emitAsync(
                                PlayerPanelPresenter.Event.ToggleTrackSaved(trackId = trackId, save = it),
                            )
                        }
                    }

                    StarRating(
                        rating = trackRating,
                        enabled = track.id != null,
                        onRate = { rating ->
                            track.id?.let { trackId ->
                                presenter.emitAsync(
                                    PlayerPanelPresenter.Event.RateTrack(trackId = trackId, rating = rating),
                                )
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
                                    ToggleSaveButton(
                                        isSaved = artistsAreSaved?.get(artist.id),
                                        size = Dimens.iconTiny,
                                    ) {
                                        presenter.emitAsync(
                                            PlayerPanelPresenter.Event.ToggleArtistSaved(
                                                artistId = artistId,
                                                save = it,
                                            ),
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
                            ToggleSaveButton(isSaved = albumIsSaved, size = Dimens.iconTiny) {
                                presenter.emitAsync(
                                    PlayerPanelPresenter.Event.ToggleAlbumSaved(albumId = albumId, save = it),
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
private fun PlayerControls(state: PlayerPanelPresenter.ViewModel, presenter: PlayerPanelPresenter) {
    val controlsEnabled = !state.loadingPlayback

    val playing = state.playbackIsPlaying == true
    val shuffling = state.playbackShuffleState == true
    val repeatState = state.playbackRepeatState

    Row(
        modifier = Modifier.instrument(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        IconButton(
            enabled = controlsEnabled && !state.togglingShuffle,
            onClick = {
                presenter.emitAsync(PlayerPanelPresenter.Event.ToggleShuffle(shuffle = !shuffling))
            },
        ) {
            CachedIcon(
                name = "shuffle",
                size = Dimens.iconSmall,
                contentDescription = "Shuffle",
                tint = LocalColors.current.highlighted(highlight = shuffling),
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.skippingPrevious,
            onClick = {
                presenter.emitAsync(PlayerPanelPresenter.Event.SkipPrevious)
            },
        ) {
            CachedIcon(name = "skip-previous", size = Dimens.iconSmall, contentDescription = "Previous")
        }

        IconButton(
            enabled = controlsEnabled && !state.togglingPlayback,
            onClick = {
                presenter.emitAsync(
                    if (playing) {
                        PlayerPanelPresenter.Event.Pause
                    } else {
                        PlayerPanelPresenter.Event.Play
                    },
                )
            },
        ) {
            CachedIcon(
                name = if (playing) "pause-circle-outline" else "play-circle-outline",
                contentDescription = if (playing) "Pause" else "Play",
            )
        }

        IconButton(
            enabled = controlsEnabled && !state.skippingNext,
            onClick = {
                presenter.emitAsync(PlayerPanelPresenter.Event.SkipNext)
            },
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

                presenter.emitAsync(PlayerPanelPresenter.Event.SetRepeat(repeatState = newRepeatState))
            },
        ) {
            CachedIcon(
                name = if (repeatState == "track") "repeat-one" else "repeat",
                size = Dimens.iconSmall,
                contentDescription = "Repeat",
                tint = LocalColors.current.highlighted(highlight = repeatState == "track" || repeatState == "context"),
            )
        }
    }
}

@Composable
private fun TrackProgress(state: PlayerPanelPresenter.ViewModel, presenter: PlayerPanelPresenter) {
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
            leftContent = {
                Text(text = remember(progress) { formatDuration(progress) }, style = MaterialTheme.typography.overline)
            },
            rightContent = {
                Text(
                    text = remember(track.durationMs) { formatDuration(track.durationMs) },
                    style = MaterialTheme.typography.overline,
                )
            },
            onSeek = { seekPercent ->
                val positionMs = (seekPercent * track.durationMs).roundToInt()
                seekProgress.value = positionMs
                presenter.emitAsync(PlayerPanelPresenter.Event.SeekTo(positionMs = positionMs))
            },
        )
    }
}

@Composable
private fun VolumeControls(state: PlayerPanelPresenter.ViewModel, presenter: PlayerPanelPresenter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val currentDevice = state.devices?.firstOrNull()

        val volumeState = remember(currentDevice?.id, currentDevice?.volumePercent) {
            mutableStateOf(currentDevice?.volumePercent)
        }
        val volume = volumeState.value
        val muted = volume == 0

        SeekableSlider(
            progress = volume?.let { it.toFloat() / 100 },
            sliderWidth = VOLUME_SLIDER_WIDTH,
            leftContent = {
                IconButton(
                    modifier = Modifier.size(Dimens.iconSmall),
                    enabled = volume != null,
                    onClick = {
                        if (volume != null) {
                            presenter.emitAsync(
                                PlayerPanelPresenter.Event.ToggleMuteVolume(mute = !muted, previousVolume = volume),
                            )
                        }
                    },
                ) {
                    CachedIcon(
                        name = if (muted) "volume-off" else "volume-up",
                        contentDescription = "Volume",
                        size = Dimens.iconSmall,
                    )
                }
            },
            onSeek = { seekPercent ->
                val volumeInt = (seekPercent * 100).roundToInt()
                volumeState.value = volumeInt
                presenter.emitAsync(PlayerPanelPresenter.Event.SetVolume(volumeInt))
            },
        )

        val refreshing = state.loadingDevices || state.loadingPlayback || state.loadingTrackPlayback
        IconButton(
            enabled = !refreshing,
            onClick = {
                presenter.emitAsync(
                    PlayerPanelPresenter.Event.LoadDevices(),
                    PlayerPanelPresenter.Event.LoadPlayback(),
                    PlayerPanelPresenter.Event.LoadTrackPlayback(),
                )
            },
        ) {
            RefreshIcon(refreshing = refreshing)
        }

        val errors = presenter.errors
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
                        onClick = { presenter.errors = emptyList() },
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceControls(state: PlayerPanelPresenter.ViewModel, presenter: PlayerPanelPresenter) {
    val devices = state.devices
    val currentDevice = state.currentDevice
    val dropdownEnabled = devices != null && devices.size > 1
    val dropdownExpanded = remember { mutableStateOf(false) }

    SimpleTextButton(
        enabled = dropdownEnabled,
        onClick = { dropdownExpanded.value = !dropdownExpanded.value },
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
                            presenter.emitAsync(PlayerPanelPresenter.Event.SelectDevice(device = device))
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
