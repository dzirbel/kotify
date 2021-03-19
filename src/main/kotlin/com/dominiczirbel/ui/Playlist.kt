package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private data class PlaylistState(
    val playlist: FullPlaylist,
    val playlistUpdated: Long?
)

@Composable
fun BoxScope.Playlist(page: PlaylistPage) {
    val refreshing = remember { mutableStateOf(false) }
    val sharedFlow = remember { MutableSharedFlow<Unit>() }
    val state = RemoteState.of(sharedFlow = sharedFlow, key = page) {
        val playlist = SpotifyCache.Playlists.getFullPlaylist(id = page.playlistId)
        refreshing.value = false

        PlaylistState(
            playlist = playlist,
            playlistUpdated = SpotifyCache.lastUpdated(id = page.playlistId)
        )
    }

    ScrollingPage(state) { playlistState ->
        val playlist = playlistState.playlist
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(playlist.name, fontSize = Dimens.fontTitle)

                    playlist.owner.displayName?.let {
                        Spacer(Modifier.width(Dimens.space3))
                        Text("by $it")
                    }

                    Spacer(Modifier.width(Dimens.space3))
                    Text("${playlist.followers.total} followers")
                }

                Column {
                    InvalidateButton(
                        refreshing = refreshing,
                        updated = playlistState.playlistUpdated,
                        onClick = {
                            SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)
                            runBlocking { sharedFlow.emit(Unit) }
                        }
                    )
                }
            }

            playlist.description?.let {
                Spacer(Modifier.height(Dimens.space3))
                Text(it)
            }

            // TODO load playlist tracks
        }
    }
}
