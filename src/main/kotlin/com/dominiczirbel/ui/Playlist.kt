package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.theme.Dimens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

private class PlaylistPresenter(private val playlistId: String) : Presenter<
    PlaylistPresenter.State,
    PlaylistPresenter.Event,
    PlaylistPresenter.Result>(key = playlistId, startingEvents = listOf(Event.Load)) {

    data class State(
        val refreshingPlaylist: Boolean = false,
        val playlist: FullPlaylist? = null,
        val tracks: List<PlaylistTrack>? = null,
        val playlistUpdated: Long? = null
    )

    sealed class Event {
        object Load : Event()
    }

    sealed class Result {
        object LoadStart : Result()
        class PlaylistLoaded(val playlist: FullPlaylist, val playlistUpdated: Long?) : Result()
        class TracksLoaded(val tracks: List<PlaylistTrack>) : Result()
    }

    override val initialState = State()

    override fun reactTo(event: Event): Flow<Result> {
        return when (event) {
            Event.Load -> flow {
                emit(Result.LoadStart)

                val playlist = SpotifyCache.Playlists.getFullPlaylist(id = playlistId)
                val playlistUpdated = SpotifyCache.lastUpdated(id = playlistId)

                emit(Result.PlaylistLoaded(playlist = playlist, playlistUpdated = playlistUpdated))

                val tracks = playlist.tracks.fetchAll<PlaylistTrack>()

                emit(Result.TracksLoaded(tracks = tracks))
            }
        }
    }

    override fun apply(state: State, result: Result): State {
        return when (result) {
            is Result.LoadStart -> state.copy(refreshingPlaylist = true)
            is Result.PlaylistLoaded -> state.copy(
                playlist = result.playlist,
                playlistUpdated = result.playlistUpdated,
                refreshingPlaylist = false
            )
            is Result.TracksLoaded -> state.copy(tracks = result.tracks)
        }
    }
}

@Composable
fun BoxScope.Playlist(page: PlaylistPage) {
    val presenter = remember(page) { PlaylistPresenter(playlistId = page.playlistId) }

    ScrollingPage(
        state = presenter.state(),
        isLoading = { it.playlist == null }
    ) { playlistState ->
        val playlist = playlistState.playlist!!
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
                        refreshing = mutableStateOf(playlistState.refreshingPlaylist),
                        updated = playlistState.playlistUpdated,
                        onClick = {
                            SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)
                            runBlocking { presenter.events.emit(PlaylistPresenter.Event.Load) }
                        }
                    )
                }
            }

            playlist.description?.let {
                Spacer(Modifier.height(Dimens.space3))
                Text(it)
            }

            Spacer(Modifier.height(Dimens.space3))

            val tracks = playlistState.tracks
            if (tracks == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                tracks.forEach { track ->
                    Text(text = track.track.name)
                }
            }
        }
    }
}
