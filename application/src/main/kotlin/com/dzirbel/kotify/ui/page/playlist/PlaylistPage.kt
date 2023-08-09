package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.asComparator
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.VerticalScrollPage
import com.dzirbel.kotify.ui.properties.PlaylistTrackAddedAtProperty
import com.dzirbel.kotify.ui.properties.PlaylistTrackIndexProperty
import com.dzirbel.kotify.ui.properties.TrackAlbumProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.rememberRatingStates
import com.dzirbel.kotify.ui.util.rememberSavedStates
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.immutable.orEmpty
import com.dzirbel.kotify.util.immutable.persistentListOfNotNull
import com.dzirbel.kotify.util.takingIf
import com.dzirbel.kotify.util.time.formatMediumDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PlaylistPage(private val playlistId: String) : Page<String?>() {
    @Composable
    override fun BoxScope.bind(visible: Boolean): String? {
        val playlist = PlaylistRepository.stateOf(id = playlistId).collectAsState().value?.cachedValue

        val playlistTracksAdapter = rememberListAdapterState(
            key = playlistId,
            defaultSort = PlaylistTrackIndexProperty,
            source = { scope ->
                PlaylistTracksRepository.stateOf(id = playlistId).mapIn(scope) { cacheState ->
                    cacheState?.cachedValue?.also { playlistTracks ->
                        // request albums and artist for tracks (but not episodes)
                        val tracks = playlistTracks.mapNotNull { it.track }
                        tracks.requestBatched(
                            transactionName = { "$it playlist track albums" },
                            extractor = { it.album },
                        )
                        tracks.requestBatched(
                            transactionName = { "$it playlist track artists" },
                            extractor = { it.artists },
                        )
                    }
                }
            },
        )

        val columns = remember(playlist) { playlistTrackColumns(playlist) }

        SavedTrackRepository.rememberSavedStates(playlistTracksAdapter.value) { it.track?.id }
        TrackRatingRepository.rememberRatingStates(playlistTracksAdapter.value) { it.track?.id }

        VerticalScrollPage(
            visible = visible,
            onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
            header = {
                PlaylistHeader(
                    playlistId = playlistId,
                    playlist = playlist,
                    sortableProperties = columns.sortableProperties(),
                    adapter = playlistTracksAdapter.value,
                    onSetSort = { playlistTracksAdapter.mutate { withSort(it) } },
                )
            },
            content = {
                if (playlistTracksAdapter.value.hasElements) {
                    Table(
                        columns = columns,
                        items = playlistTracksAdapter.value,
                        onSetSort = { playlistTracksAdapter.mutate { withSort(persistentListOfNotNull(it)) } },
                    )
                } else {
                    PageLoadingSpinner()
                }
            },
        )

        return playlist?.name
    }

    override fun titleFor(data: String?) = data
}

@Composable
private fun PlaylistHeader(
    playlistId: String,
    playlist: PlaylistViewModel?,
    sortableProperties: ImmutableList<SortableProperty<PlaylistTrackViewModel>>,
    adapter: ListAdapter<PlaylistTrackViewModel>,
    onSetSort: (PersistentList<Sort<PlaylistTrackViewModel>>) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadedImage(playlist?.largestImageUrl)

            if (playlist != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(playlist.name, style = MaterialTheme.typography.h5)

                    playlist.description?.takeIf { it.isNotEmpty() }?.let {
                        Text(it)
                    }

                    val owner = UserRepository.stateOf(id = playlist.ownerId).collectAsState().value?.cachedValue
                    Text("Created by ${owner?.name ?: "..."}; ${playlist.followersTotal} followers")

                    val totalDuration = takingIf(adapter.hasElements) {
                        remember(adapter) {
                            adapter
                                .sumOf { it.track?.durationMs ?: it.episode?.durationMs ?: 0 }
                                .milliseconds
                                .formatMediumDuration()
                        }
                    }

                    Text("${playlist.totalTracks} songs" + totalDuration?.let { ", $it" })

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToggleSaveButton(
                            repository = SavedPlaylistRepository,
                            id = playlistId,
                            size = Dimens.iconMedium,
                        )

                        PlayButton(context = Player.PlayContext.playlist(playlist))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                        SortSelector(
                            sortableProperties = sortableProperties,
                            sorts = adapter.sorts.orEmpty(),
                            onSetSort = onSetSort,
                        )

                        PlaylistReorderButton(
                            enabled = !adapter.sorts.isNullOrEmpty() && adapter.hasElements,
                            reorder = {
                                PlaylistTracksRepository.reorder(
                                    playlistId = playlistId,
                                    tracks = adapter.toList(),
                                    comparator = requireNotNull(adapter.sorts).asComparator(),
                                )
                            },
                            // TODO doesn't seem quite right... just revert to order by index on playlist?
                            onReorderFinish = { onSetSort(persistentListOf()) },
                        )
                    }
                }
            }
        }

        Row {
            InvalidateButton(repository = PlaylistRepository, id = playlistId, entityName = "Playlist")
            InvalidateButton(repository = PlaylistTracksRepository, id = playlistId, entityName = "Tracks")
        }
    }
}

@Composable
private fun PlaylistReorderButton(
    enabled: Boolean,
    reorder: () -> Flow<PlaylistTracksRepository.PlaylistReorderState>,
    onReorderFinish: () -> Unit,
) {
    val reorderState = remember { mutableStateOf<PlaylistTracksRepository.PlaylistReorderState?>(null) }

    Button(
        enabled = enabled && reorderState.value == null,
        onClick = {
            // TODO prevent sort changes while reordering?
            Repository.applicationScope.launch {
                reorder()
                    .onCompletion {
                        reorderState.value = null
                        onReorderFinish()
                    }
                    .collect { state -> reorderState.value = state }
            }
        },
    ) {
        val text = when (val state = reorderState.value) {
            PlaylistTracksRepository.PlaylistReorderState.Calculating -> "Calculating"

            is PlaylistTracksRepository.PlaylistReorderState.Reordering ->
                "Reordering ${state.completedOps} / ${state.totalOps}"

            PlaylistTracksRepository.PlaylistReorderState.Verifying -> "Verifying"

            null -> "Set current order as playlist order"
        }

        Text(text)
    }
}

private fun playlistTrackColumns(playlist: PlaylistViewModel?): PersistentList<Column<PlaylistTrackViewModel>> {
    return persistentListOf(
        TrackPlayingColumn(
            trackIdOf = { playlistTrack -> playlistTrack.track?.id },
            playContextFromTrack = { playlistTrack ->
                playlist?.let {
                    // TODO use current sort order instead of playlist order?
                    Player.PlayContext.playlistTrack(playlist, playlistTrack.indexOnPlaylist)
                }
            },
        ),
        PlaylistTrackIndexProperty,
        TrackSavedProperty(trackIdOf = { playlistTrack -> playlistTrack.track?.id }),
        TrackNameProperty.ForPlaylistTrack,
        TrackArtistsProperty.ForPlaylistTrack,
        TrackAlbumProperty.ForPlaylistTrack,
        TrackRatingProperty(trackIdOf = { playlistTrack -> playlistTrack.track?.id }),
        PlaylistTrackAddedAtProperty,
        TrackDurationProperty.ForPlaylistTrack,
        TrackPopularityProperty.ForPlaylistTrack,
    )
}
