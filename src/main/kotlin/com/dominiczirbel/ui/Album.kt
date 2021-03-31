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
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private data class AlbumState(
    val album: FullAlbum,
    val tracks: List<SimplifiedTrack>,
    val albumUpdated: Long?
)

private val AlbumTrackColumns = StandardTrackColumns.minus(AlbumColumn)

@Composable
fun BoxScope.Album(page: AlbumPage) {
    val refreshing = remember { mutableStateOf(false) }

    val sharedFlow = remember { MutableSharedFlow<Unit>() }
    val remoteState = RemoteState.of(sharedFlow = sharedFlow, key = page) {
        val album = SpotifyCache.Albums.getFullAlbum(page.albumId)
        val tracks = album.tracks.fetchAll<SimplifiedTrack>()

        AlbumState(
            album = album,
            tracks = tracks,
            albumUpdated = SpotifyCache.lastUpdated(id = page.albumId)
        )
    }

    ScrollingPage(remoteState) { state ->
        val album = state.album
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(album.name, fontSize = Dimens.fontTitle)

                InvalidateButton(
                    refreshing = refreshing,
                    updated = state.albumUpdated,
                    updatedFormat = { "Album last updated $it" },
                    updatedFallback = "Album never updated",
                    onClick = {
                        SpotifyCache.invalidate(page.albumId)
                        runBlocking { sharedFlow.emit(Unit) }
                    }
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
