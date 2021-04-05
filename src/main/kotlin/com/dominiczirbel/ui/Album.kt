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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope

private class AlbumPresenter(private val albumId: String, scope: CoroutineScope) :
    Presenter<AlbumPresenter.State?, AlbumPresenter.Event>(
        scope = scope,
        key = albumId,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val album: FullAlbum,
        val tracks: List<Track>,
        val albumUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SpotifyCache.invalidate(id = albumId)
                }

                val album = SpotifyCache.Albums.getFullAlbum(albumId)
                val tracks = album.tracks.fetchAll<SimplifiedTrack>()

                mutateState {
                    State(
                        refreshing = false,
                        album = album,
                        tracks = tracks,
                        albumUpdated = SpotifyCache.lastUpdated(id = albumId)
                    )
                }

                val fullTracks = SpotifyCache.Tracks.getFullTracks(ids = tracks.map { it.id!! }, scope = scope)

                mutateState { it?.copy(tracks = fullTracks) }
            }
        }
    }
}

private val AlbumTrackColumns = StandardTrackColumns.minus(AlbumColumn)

@Composable
fun BoxScope.Album(page: AlbumPage) {
    val scope = rememberCoroutineScope()
    val presenter = remember(page) { AlbumPresenter(albumId = page.albumId, scope = scope) }

    ScrollingPage(state = { presenter.state() }) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.album.name, fontSize = Dimens.fontTitle)

                InvalidateButton(
                    refreshing = state.refreshing,
                    updated = state.albumUpdated,
                    updatedFormat = { "Album last updated $it" },
                    updatedFallback = "Album never updated",
                    onClick = { presenter.emitEvent(AlbumPresenter.Event.Load(invalidate = true)) }
                )
            }

            Spacer(Modifier.height(Dimens.space3))

            Table(
                columns = AlbumTrackColumns,
                items = state.tracks
            )
        }
    }
}
