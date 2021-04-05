package com.dominiczirbel.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.Playlist
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.common.SimpleTextButton
import com.dominiczirbel.ui.common.VerticalScroll
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class LibraryPresenter(scope: CoroutineScope) :
    Presenter<RemoteState<LibraryPresenter.State>, LibraryPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load),
        initialState = RemoteState.Loading()
    ) {

    data class State(val refreshing: Boolean, val playlists: List<Playlist>)

    sealed class Event {
        object Load : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                mutateRemoteState { it.copy(refreshing = true) }

                val playlists = SpotifyCache.Playlists.getSavedPlaylists()
                    .map { SpotifyCache.Playlists.getPlaylist(it) }

                mutateState {
                    RemoteState.Success(
                        State(
                            refreshing = false,
                            playlists = playlists
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryPanel(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { LibraryPresenter(scope = scope) }

    VerticalScroll {
        Text(
            text = "Library",
            fontSize = Dimens.fontTitle,
            modifier = Modifier.padding(Dimens.space3)
        )

        Spacer(Modifier.height(Dimens.space3))

        MaxWidthButton(
            text = "Artists",
            selected = pageStack.value.current == ArtistsPage,
            onClick = { pageStack.mutate { to(ArtistsPage) } }
        )

        MaxWidthButton(
            text = "Albums",
            selected = pageStack.value.current == AlbumsPage,
            onClick = { pageStack.mutate { to(AlbumsPage) } }
        )

        MaxWidthButton(
            text = "Songs",
            selected = pageStack.value.current == TracksPage,
            onClick = { pageStack.mutate { to(TracksPage) } }
        )

        Spacer(Modifier.height(Dimens.space3))

        Text(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space3),
            fontSize = Dimens.fontTitle,
            text = "Playlists"
        )

        when (val state = presenter.state()) {
            is RemoteState.Error ->
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterHorizontally),
                    tint = Colors.current.error
                )

            is RemoteState.Loading ->
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterHorizontally)
                )

            is RemoteState.Success ->
                state.data.playlists.forEach { playlist ->
                    MaxWidthButton(
                        text = playlist.name,
                        contentPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
                        selected = pageStack.value.current == PlaylistPage(playlistId = playlist.id),
                        onClick = { pageStack.mutate { to(PlaylistPage(playlistId = playlist.id)) } }
                    )
                }
        }
    }
}

@Composable
private fun MaxWidthButton(
    text: String,
    selected: Boolean,
    contentPadding: PaddingValues = PaddingValues(all = Dimens.space3),
    onClick: () -> Unit
) {
    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
