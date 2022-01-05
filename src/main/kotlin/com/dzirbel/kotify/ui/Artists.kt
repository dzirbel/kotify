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
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.plusOrMinus
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
        val artists: List<Artist>,
        val savedArtistIds: Set<String>,
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
                    SavedArtistRepository.invalidateLibrary()
                }

                val savedArtistIds = SavedArtistRepository.getLibrary()
                val artists = ArtistRepository.getFull(ids = savedArtistIds.toList())
                    .filterNotNull()
                    .sortedBy { it.name }
                val artistsUpdated = SavedArtistRepository.libraryUpdated()

                SpotifyImageCache.loadFromFileCache(
                    urls = artists.mapNotNull { it.images.firstOrNull()?.url },
                    scope = scope,
                )

                mutateState {
                    State(
                        refreshing = false,
                        artists = artists,
                        savedArtistIds = savedArtistIds,
                        artistsUpdated = artistsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ToggleSave -> {
                SavedArtistRepository.setSaved(id = event.artistId, saved = event.save)
                mutateState {
                    it?.copy(savedArtistIds = it.savedArtistIds.plusOrMinus(event.artistId, event.save))
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
                    savedArtists = state.savedArtistIds,
                    presenter = presenter,
                    pageStack = pageStack
                )
            }
        }
    }
}

@Composable
private fun ArtistCell(
    artist: Artist,
    savedArtists: Set<String>,
    presenter: ArtistsPresenter,
    pageStack: MutableState<PageStack>,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(ArtistPage(artistId = artist.id.value)) } }
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

            val isSaved = savedArtists.contains(artist.id.value)
            ToggleSaveButton(isSaved = isSaved) {
                presenter.emitAsync(ArtistsPresenter.Event.ToggleSave(artistId = artist.id.value, save = !isSaved))
            }

            PlayButton(context = Player.PlayContext.artist(artist), size = Dimens.iconSmall)
        }
    }
}
