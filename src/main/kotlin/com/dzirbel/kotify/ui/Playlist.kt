package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.IndexColumn
import com.dzirbel.kotify.ui.components.table.Sort
import com.dzirbel.kotify.ui.components.table.SortSelector
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.ReorderCalculator
import com.dzirbel.kotify.util.compareInOrder
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.util.concurrent.TimeUnit

private class PlaylistPresenter(
    private val page: PlaylistPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope,
) : Presenter<PlaylistPresenter.ViewModel?, PlaylistPresenter.Event>(
    scope = scope,
    key = page.playlistId,
    eventMergeStrategy = EventMergeStrategy.LATEST,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = null
) {

    data class ViewModel(
        val refreshing: Boolean,
        val reordering: Boolean = false,
        val sorts: List<Sort<PlaylistTrack>> = emptyList(),
        val playlist: Playlist,
        val tracks: List<PlaylistTrack>?,
        val trackRatings: Map<String, State<Rating?>>?,
        val savedTracksState: State<Set<String>?>,
        val isSavedState: State<Boolean?>,
        val playlistUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val save: Boolean) : Event()
        data class ToggleTrackSaved(val trackId: String, val saved: Boolean) : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()
        data class SetSorts(val sorts: List<Sort<PlaylistTrack>>) : Event()
        data class Order(val sorts: List<Sort<PlaylistTrack>>, val tracks: List<PlaylistTrack>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    PlaylistRepository.invalidate(id = page.playlistId)
                    KotifyDatabase.transaction { PlaylistTrack.invalidate(playlistId = page.playlistId) }
                }

                val playlist = PlaylistRepository.getFull(id = page.playlistId)
                    ?: error("TODO show 404 page") // TODO 404 page
                pageStack.mutate { withPageTitle(title = page.titleFor(playlist)) }
                val playlistUpdated = playlist.updatedTime.toEpochMilli()
                KotifyDatabase.transaction {
                    playlist.owner.loadToCache()
                    playlist.largestImage.loadToCache()
                }

                val isSavedState = SavedPlaylistRepository.savedStateOf(id = playlist.id.value, fetchIfUnknown = true)
                val savedTracksState = SavedTrackRepository.libraryState()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        playlist = playlist,
                        playlistUpdated = playlistUpdated,
                        isSavedState = isSavedState,
                        tracks = null,
                        trackRatings = null,
                        savedTracksState = savedTracksState,
                    )
                }

                val tracks = playlist.getAllTracks()
                loadTracksToCache(tracks)
                val trackRatings = tracks
                    .map { it.track.cached.id.value }
                    .associateWith { trackId -> TrackRatingRepository.ratingState(trackId) }

                mutateState { it?.copy(tracks = tracks, trackRatings = trackRatings) }
            }

            is Event.ToggleSave -> SavedPlaylistRepository.setSaved(id = page.playlistId, saved = event.save)

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.saved)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)

            is Event.SetSorts -> mutateState { it?.copy(sorts = event.sorts) }

            is Event.Order -> {
                val ops = ReorderCalculator.calculateReorderOperations(
                    list = event.tracks.withIndex().toList(),
                    comparator = event.sorts.map { it.comparator }.compareInOrder(),
                )

                if (ops.isNotEmpty()) {
                    mutateState { it?.copy(reordering = true) }

                    for (op in ops) {
                        Spotify.Playlists.reorderPlaylistItems(
                            playlistId = page.playlistId,
                            rangeStart = op.rangeStart,
                            rangeLength = op.rangeLength,
                            insertBefore = op.insertBefore,
                        )
                    }

                    KotifyDatabase.transaction { PlaylistTrack.invalidate(playlistId = page.playlistId) }
                    val playlist = PlaylistRepository.getCached(id = page.playlistId)
                    val tracks = playlist?.getAllTracks()
                    tracks?.let { loadTracksToCache(it) }

                    mutateState { it?.copy(tracks = tracks, reordering = false, sorts = emptyList()) }
                }
            }
        }
    }

    private suspend fun loadTracksToCache(tracks: List<PlaylistTrack>) {
        KotifyDatabase.transaction {
            tracks.forEach {
                it.track.loadToCache()
                it.track.cached.artists.loadToCache()
                it.track.cached.album.loadToCache()
            }
        }
    }
}

private object AddedAtColumn : ColumnByString<PlaylistTrack>(name = "Added") {
    // TODO precompute rather than re-parsing each time this is accessed
    private val PlaylistTrack.addedAtTimestamp
        get() = Instant.parse(addedAt.orEmpty()).toEpochMilli()

    override fun toString(item: PlaylistTrack, index: Int): String {
        return formatDateTime(timestamp = item.addedAtTimestamp, includeTime = false)
    }

    override fun compare(first: PlaylistTrack, firstIndex: Int, second: PlaylistTrack, secondIndex: Int): Int {
        return first.addedAtTimestamp.compareTo(second.addedAtTimestamp)
    }
}

@Composable
fun BoxScope.Playlist(pageStack: MutableState<PageStack>, page: PlaylistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { PlaylistPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadedImage(url = state.playlist.largestImage.cached?.url)

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(state.playlist.name, fontSize = Dimens.fontTitle)

                    state.playlist.description
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { Text(it) }

                    Text(
                        "Created by ${state.playlist.owner.cached.name}; " +
                            "${state.playlist.followersTotal} followers"
                    )

                    val totalDurationMins = remember(state.tracks) {
                        state.tracks?.let { tracks ->
                            TimeUnit.MILLISECONDS.toMinutes(
                                tracks.sumOf { it.track.cached.durationMs.toInt() }.toLong()
                            )
                        }
                    }

                    Text("${state.playlist.totalTracks} songs, ${totalDurationMins ?: "<loading>"} min")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleSaveButton(isSaved = state.isSavedState.value, size = Dimens.iconMedium) {
                            presenter.emitAsync(PlaylistPresenter.Event.ToggleSave(save = it))
                        }

                        PlayButton(context = Player.PlayContext.playlist(state.playlist))
                    }

                    Button(
                        enabled = state.sorts.isNotEmpty() && state.tracks != null && !state.reordering,
                        onClick = {
                            state.tracks?.let { tracks ->
                                presenter.emitAsync(
                                    PlaylistPresenter.Event.Order(sorts = state.sorts, tracks = tracks)
                                )
                            }
                        },
                    ) {
                        if (state.reordering) {
                            Text("Reordering...")
                        } else {
                            Text("Set current order as playlist order")
                        }
                    }
                }
            }

            InvalidateButton(
                refreshing = state.refreshing,
                updated = state.playlistUpdated,
                updatedFormat = { "Playlist last updated $it" },
                updatedFallback = "Playlist never updated",
                onClick = { presenter.emitAsync(PlaylistPresenter.Event.Load(invalidate = true)) }
            )
        }

        VerticalSpacer(Dimens.space3)

        val tracks = state.tracks
        if (tracks == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val columns = remember(pageStack) {
                trackColumns(
                    pageStack = pageStack,
                    savedTracks = state.savedTracksState.value,
                    onSetTrackSaved = { trackId, saved ->
                        presenter.emitAsync(
                            PlaylistPresenter.Event.ToggleTrackSaved(trackId = trackId, saved = saved)
                        )
                    },
                    trackRatings = state.trackRatings,
                    onRateTrack = { trackId, rating ->
                        presenter.emitAsync(PlaylistPresenter.Event.RateTrack(trackId = trackId, rating = rating))
                    },
                    includeTrackNumber = false,
                    playContextFromIndex = { index ->
                        Player.PlayContext.playlistTrack(playlist = state.playlist, index = index)
                    }
                )
                    .map { column -> column.mapped<PlaylistTrack> { it.track.cached } }
                    .toMutableList()
                    .apply {
                        add(1, IndexColumn())

                        @Suppress("MagicNumber")
                        add(6, AddedAtColumn)
                    }
            }

            // TODO move into playlist header and align right
            SortSelector(
                columns = columns,
                sorts = state.sorts,
                onSetSort = { sorts -> presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = sorts)) }
            )

            Table(
                columns = columns,
                items = tracks,
                sorts = state.sorts,
                onSetSort = { sort ->
                    presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = listOfNotNull(sort)))
                },
            )
        }
    }
}
