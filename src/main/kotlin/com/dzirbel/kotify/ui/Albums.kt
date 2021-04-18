package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.ui.common.Grid
import com.dzirbel.kotify.ui.common.InvalidateButton
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class AlbumsPresenter(scope: CoroutineScope) :
    Presenter<AlbumsPresenter.State?, AlbumsPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val albums: List<Album>,
        val albumsUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedAlbums.ID)
                }

                val albums = SpotifyCache.Albums.getSavedAlbums()
                    .map { SpotifyCache.Albums.getAlbum(it) }
                    .sortedBy { it.name }

                mutateState {
                    State(
                        refreshing = false,
                        albums = albums,
                        albumsUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedAlbums.ID)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.Albums(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { AlbumsPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Albums", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = state.refreshing,
                        updated = state.albumsUpdated,
                        onClick = { presenter.emitAsync(AlbumsPresenter.Event.Load(invalidate = true)) }
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = state.albums,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                verticalCellAlignment = Alignment.Top
            ) { album -> AlbumCell(album, pageStack) }
        }
    }
}
