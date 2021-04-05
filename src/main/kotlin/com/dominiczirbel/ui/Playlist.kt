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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.ui.common.ColumnByString
import com.dominiczirbel.ui.common.ColumnWidth
import com.dominiczirbel.ui.common.IndexColumn
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.util.formatDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Instant

private class PlaylistPresenter(private val playlistId: String, scope: CoroutineScope) :
    Presenter<PlaylistPresenter.State?, PlaylistPresenter.Event>(
        scope = scope,
        key = playlistId,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val playlist: FullPlaylist,
        val tracks: List<PlaylistTrack>?,
        val playlistUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SpotifyCache.invalidate(id = playlistId)
                }

                val playlist = SpotifyCache.Playlists.getFullPlaylist(id = playlistId)

                mutateState {
                    State(
                        refreshing = false,
                        playlist = playlist,
                        playlistUpdated = SpotifyCache.lastUpdated(id = playlistId),
                        tracks = null
                    )
                }

                val tracks = playlist.tracks.fetchAll<PlaylistTrack>()

                mutateState { it?.copy(tracks = tracks) }
            }
        }
    }
}

private object AddedAtColumn : ColumnByString<PlaylistTrack>(header = "Added", width = ColumnWidth.Fill()) {
    private val PlaylistTrack.addedAtTimestamp
        get() = Instant.parse(addedAt.orEmpty()).toEpochMilli()

    override fun toString(item: PlaylistTrack, index: Int): String {
        return formatDateTime(timestamp = item.addedAtTimestamp, includeTime = false)
    }

    override fun compare(first: PlaylistTrack, firstIndex: Int, second: PlaylistTrack, secondIndex: Int): Int {
        return first.addedAtTimestamp.compareTo(second.addedAtTimestamp)
    }
}

private val PlaylistColumns = StandardTrackColumns
    .minus(TrackNumberColumn)
    .map { column -> column.mapped<PlaylistTrack> { it.track } }
    .toMutableList()
    .apply {
        add(0, IndexColumn)

        @Suppress("MagicNumber")
        add(4, AddedAtColumn)
    }

@Composable
fun BoxScope.Playlist(page: PlaylistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { PlaylistPresenter(playlistId = page.playlistId, scope = scope) }

    ScrollingPage(state = { presenter.state() }) { state ->
        val playlist = state.playlist
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
                        refreshing = state.refreshing,
                        updated = state.playlistUpdated,
                        onClick = { presenter.emitAsync(PlaylistPresenter.Event.Load(invalidate = true)) }
                    )
                }
            }

            playlist.description?.let {
                Spacer(Modifier.height(Dimens.space3))
                Text(it)
            }

            Spacer(Modifier.height(Dimens.space3))

            val tracks = state.tracks
            if (tracks == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Table(columns = PlaylistColumns, items = tracks)
            }
        }
    }
}
