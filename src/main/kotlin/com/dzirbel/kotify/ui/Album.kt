package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Instant
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
    initialState = null,
) {

    data class State(
        val refreshing: Boolean,
        val album: Album,
        val tracks: List<Track>,
        val isSaved: Boolean?,
        val albumUpdated: Instant,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val save: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    AlbumRepository.invalidate(id = page.albumId)
                }

                val album = AlbumRepository.get(id = page.albumId) ?: error("TODO show 404 page") // TODO 404 page

                pageStack.mutate { withPageTitle(title = page.titleFor(album)) }

                val isSaved = KotifyDatabase.transaction { album.isSaved }
                val tracks = album.getAllTracks()

                mutateState {
                    State(
                        refreshing = false,
                        album = album,
                        tracks = tracks,
                        isSaved = isSaved,
                        albumUpdated = album.updatedTime,
                    )
                }

                val fullTracks = TrackRepository.getFull(ids = tracks.map { it.id.value })
                    .zip(tracks) { fullTrack, existingTrack -> fullTrack ?: existingTrack }

                mutateState { it?.copy(tracks = fullTracks) }
            }

            is Event.ToggleSave -> {
                SavedAlbumRepository.setSaved(id = page.albumId, saved = event.save)
                mutateState { it?.copy(isSaved = event.save) }
            }
        }
    }
}

@Composable
fun BoxScope.Album(pageStack: MutableState<PageStack>, page: AlbumPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { AlbumPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
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
                                link(text = artist.name, link = artist.id.value)
                            }
                        }

                        state.album.releaseDate?.let {
                            Text(it)
                        }

                        val totalDurationMins = remember(state.tracks) {
                            TimeUnit.MILLISECONDS.toMinutes(state.tracks.sumOf { it.durationMs.toInt() }.toLong())
                        }

                        Text("${state.album.totalTracks} songs, $totalDurationMins min")

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToggleSaveButton(isSaved = state.isSaved, size = Dimens.iconMedium) {
                                presenter.emitAsync(AlbumPresenter.Event.ToggleSave(save = it))
                            }

                            PlayButton(context = Player.PlayContext.album(state.album))
                        }
                    }
                }

                InvalidateButton(
                    refreshing = state.refreshing,
                    updated = state.albumUpdated.toEpochMilli(),
                    updatedFormat = { "Album last updated $it" },
                    updatedFallback = "Album never updated",
                    onClick = { presenter.emitAsync(AlbumPresenter.Event.Load(invalidate = true)) }
                )
            }

            VerticalSpacer(Dimens.space3)

            Table(
                columns = trackColumns2(
                    pageStack = pageStack,
                    includeAlbum = false,
                    playContextFromIndex = { index ->
                        Player.PlayContext.albumTrack(album = state.album, index = index)
                    }
                ),
                items = state.tracks
            )
        }
    }
}
