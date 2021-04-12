package com.dominiczirbel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.svgResource
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.ui.common.Grid
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class ArtistsPresenter(scope: CoroutineScope) :
    Presenter<ArtistsPresenter.State?, ArtistsPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val artists: List<FullArtist>,
        val artistsUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
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

                mutateState {
                    State(
                        refreshing = false,
                        artists = artists,
                        artistsUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedArtists.ID)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.Artists(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { ArtistsPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
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

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = state.artists,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                verticalCellAlignment = Alignment.Top
            ) { artist -> ArtistCell(artist, pageStack) }
        }
    }
}

@Composable
private fun ArtistCell(artist: FullArtist, pageStack: MutableState<PageStack>) {
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

        Spacer(Modifier.height(Dimens.space3))

        Row(
            Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = artist.name, modifier = Modifier.weight(1f))

            IconButton(
                enabled = Player.playable,
                modifier = Modifier.size(Dimens.iconSmall),
                onClick = { Player.play(contextUri = artist.uri) }
            ) {
                val playing = Player.playbackContext.value?.uri == artist.uri
                Icon(
                    painter = svgResource("play-circle-outline.svg"),
                    modifier = Modifier.size(Dimens.iconSmall),
                    contentDescription = "Play",
                    tint = Colors.current.highlighted(highlight = playing)
                )
            }
        }
    }
}
