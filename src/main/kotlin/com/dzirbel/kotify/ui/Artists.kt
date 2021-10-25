package com.dzirbel.kotify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.network.model.FullArtist
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class ArtistsPresenter(scope: CoroutineScope) : Presenter<ArtistsPresenter.State?, ArtistsPresenter.Event>(
    scope = scope,
    eventMergeStrategy = EventMergeStrategy.LATEST,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = null
) {
    data class State(
        val refreshing: Boolean,
        val artists: List<FullArtist>,
        val savedArtists: Set<String>,
        val artistsUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val artistId: String, val save: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)
                }

                val artists = SpotifyCache.Artists.getSavedArtists()
                    .map { SpotifyCache.Artists.getFullArtist(it) }
                    .sortedBy { it.name }

                val savedArtists = artists.mapTo(mutableSetOf()) { it.id }

                SpotifyImageCache.loadFromFileCache(
                    urls = artists.mapNotNull { it.images.firstOrNull()?.url },
                    scope = scope,
                )

                mutateState {
                    State(
                        refreshing = false,
                        artists = artists,
                        savedArtists = savedArtists,
                        artistsUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedArtists.ID)
                    )
                }
            }

            is Event.ToggleSave -> {
                val savedArtists = if (event.save) {
                    SpotifyCache.Artists.saveArtist(id = event.artistId)
                } else {
                    SpotifyCache.Artists.unsaveArtist(id = event.artistId)
                }

                savedArtists?.let {
                    mutateState { it?.copy(savedArtists = savedArtists) }
                }
            }
        }
    }
}

@Composable
fun BoxScope.Artists(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { ArtistsPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Artists", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = state.refreshing,
                        updated = state.artistsUpdated,
                        onClick = { presenter.emitAsync(ArtistsPresenter.Event.Load(invalidate = true)) }
                    )
                }
            }

            VerticalSpacer(Dimens.space3)

            Grid(
                elements = state.artists,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                cellAlignment = Alignment.TopCenter,
            ) { artist ->
                ArtistCell(
                    artist = artist,
                    savedArtists = state.savedArtists,
                    presenter = presenter,
                    pageStack = pageStack
                )
            }
        }
    }
}

@Composable
private fun ArtistCell(
    artist: FullArtist,
    savedArtists: Set<String>,
    presenter: ArtistsPresenter,
    pageStack: MutableState<PageStack>,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(ArtistPage(artistId = artist.id)) } }
            .padding(Dimens.space3)
    ) {
        LoadedImage(
            url = artist.images.firstOrNull()?.url,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        VerticalSpacer(Dimens.space3)

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = artist.name, modifier = Modifier.weight(1f))

            val isSaved = savedArtists.contains(artist.id)
            ToggleSaveButton(isSaved = isSaved) {
                presenter.emitAsync(ArtistsPresenter.Event.ToggleSave(artistId = artist.id, save = !isSaved))
            }

            PlayButton(context = Player.PlayContext.artist(artist), size = Dimens.iconSmall)
        }
    }
}
