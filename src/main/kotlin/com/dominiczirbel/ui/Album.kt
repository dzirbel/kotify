package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

private class AlbumPresenter(private val albumId: String) : Presenter<
    AlbumPresenter.State,
    AlbumPresenter.Event,
    AlbumPresenter.Result>(key = albumId, startingEvents = listOf(Event.Load)) {

    data class State(
        val loading: Boolean = false,
        val album: FullAlbum? = null,
        val tracks: List<Track>? = null,
        val albumUpdated: Long? = null
    )

    sealed class Event {
        object Load : Event()
    }

    sealed class Result {
        object LoadStart : Result()
        class AlbumLoaded(val album: FullAlbum, val albumUpdated: Long?, val tracks: List<SimplifiedTrack>) : Result()
        class TracksLoaded(val tracks: List<Track>) : Result()
    }

    override val initialState = State()

    override fun reactTo(event: Event): Flow<Result> {
        return when (event) {
            Event.Load -> flow {
                emit(Result.LoadStart)

                val album = SpotifyCache.Albums.getFullAlbum(albumId)
                val tracks = album.tracks.fetchAll<SimplifiedTrack>()

                emit(
                    Result.AlbumLoaded(
                        album = album,
                        tracks = tracks,
                        albumUpdated = SpotifyCache.lastUpdated(id = albumId)
                    )
                )

                val fullTracks = SpotifyCache.Tracks.getFullTracks(ids = tracks.map { it.id!! })
                emit(Result.TracksLoaded(tracks = fullTracks))
            }
        }
    }

    override fun apply(state: State, result: Result): State {
        return when (result) {
            Result.LoadStart -> state.copy(loading = true)
            is Result.AlbumLoaded -> state.copy(
                loading = false,
                album = result.album,
                tracks = result.tracks,
                albumUpdated = result.albumUpdated
            )
            is Result.TracksLoaded -> state.copy(tracks = result.tracks)
        }
    }
}

private val AlbumTrackColumns = StandardTrackColumns.minus(AlbumColumn)

@Composable
fun BoxScope.Album(page: AlbumPage) {
    val presenter = remember(page) { AlbumPresenter(albumId = page.albumId) }

    ScrollingPage(
        state = presenter.state(),
        isLoading = { it.album == null }
    ) { state ->
        val album = state.album!!
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(album.name, fontSize = Dimens.fontTitle)

                InvalidateButton(
                    refreshing = mutableStateOf(state.loading),
                    updated = state.albumUpdated,
                    updatedFormat = { "Album last updated $it" },
                    updatedFallback = "Album never updated",
                    onClick = {
                        SpotifyCache.invalidate(page.albumId)
                        runBlocking { presenter.events.emit(AlbumPresenter.Event.Load) }
                    }
                )
            }

            Spacer(Modifier.height(Dimens.space3))

            Table(
                columns = AlbumTrackColumns,
                items = state.tracks!!
            )
        }
    }
}
