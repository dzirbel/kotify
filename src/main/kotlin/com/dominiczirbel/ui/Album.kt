package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.LinkedText
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.TimeUnit

private class AlbumPresenter(
    private val page: AlbumPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope
) : Presenter<AlbumPresenter.State?, AlbumPresenter.Event>(
    scope = scope,
    key = page.albumId,
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
                    SpotifyCache.invalidate(id = page.albumId)
                }

                val album = SpotifyCache.Albums.getFullAlbum(page.albumId)
                pageStack.mutate { withPageTitle(title = page.titleFor(album)) }

                val tracks = album.tracks.fetchAll<SimplifiedTrack>()

                mutateState {
                    State(
                        refreshing = false,
                        album = album,
                        tracks = tracks,
                        albumUpdated = SpotifyCache.lastUpdated(id = page.albumId)
                    )
                }

                val fullTracks = SpotifyCache.Tracks.getFullTracks(ids = tracks.map { it.id!! })

                mutateState { it?.copy(tracks = fullTracks) }
            }
        }
    }
}

@Composable
fun BoxScope.Album(pageStack: MutableState<PageStack>, page: AlbumPage) {
    val scope = rememberCoroutineScope()
    val presenter = remember(page) { AlbumPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadedImage(url = state.album.images.firstOrNull()?.url)

                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                        Text(state.album.name, fontSize = Dimens.fontTitle)

                        LinkedText(
                            onClickLink = { artistId ->
                                pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                            }
                        ) {
                            text("By ")
                            list(state.album.artists) { artist ->
                                link(text = artist.name, link = artist.id)
                            }
                        }

                        Text(state.album.releaseDate)

                        val totalDurationMins = remember(state.tracks) {
                            TimeUnit.MILLISECONDS.toMinutes(state.tracks.sumBy { it.durationMs.toInt() }.toLong())
                        }

                        Text("${state.album.tracks.total} songs, $totalDurationMins min")

                        val playing = Player.isPlaying.value && Player.playbackContext.value?.uri == state.album.uri
                        IconButton(
                            enabled = Player.playable,
                            modifier = Modifier.size(Dimens.iconMedium),
                            onClick = {
                                if (playing) Player.pause() else Player.play(contextUri = state.album.uri)
                            }
                        ) {
                            Icon(
                                painter = svgResource(
                                    if (playing) "pause-circle-outline.svg" else "play-circle-outline.svg"
                                ),
                                modifier = Modifier.size(Dimens.iconMedium),
                                contentDescription = "Play"
                            )
                        }
                    }
                }

                InvalidateButton(
                    refreshing = state.refreshing,
                    updated = state.albumUpdated,
                    updatedFormat = { "Album last updated $it" },
                    updatedFallback = "Album never updated",
                    onClick = { presenter.emitAsync(AlbumPresenter.Event.Load(invalidate = true)) }
                )
            }

            Spacer(Modifier.height(Dimens.space3))

            Table(
                columns = trackColumns(pageStack, includeAlbum = false),
                items = state.tracks
            )
        }
    }
}
