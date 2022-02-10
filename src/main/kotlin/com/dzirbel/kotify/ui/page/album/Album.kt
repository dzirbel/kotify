package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.components.trackColumns
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

@Composable
fun BoxScope.Album(pageStack: MutableState<PageStack>, page: AlbumPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { AlbumPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadedImage(url = state.album.largestImage.cached?.url)

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(state.album.name, fontSize = Dimens.fontTitle)

                    LinkedText(
                        onClickLink = { artistId ->
                            pageStack.mutate { to(ArtistPage(artistId = artistId)) }
                        }
                    ) {
                        text("By ")
                        list(state.album.artists.cached) { artist ->
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
                        ToggleSaveButton(isSaved = state.isSavedState.value, size = Dimens.iconMedium) {
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
            columns = trackColumns(
                pageStack = pageStack,
                savedTracks = state.savedTracksState.value,
                onSetTrackSaved = { trackId, saved ->
                    presenter.emitAsync(AlbumPresenter.Event.ToggleTrackSaved(trackId = trackId, saved = saved))
                },
                trackRatings = state.trackRatings,
                onRateTrack = { trackId, rating ->
                    presenter.emitAsync(AlbumPresenter.Event.RateTrack(trackId = trackId, rating = rating))
                },
                includeAlbum = false,
                playContextFromIndex = { index ->
                    Player.PlayContext.albumTrack(album = state.album, index = index)
                }
            ),
            items = state.tracks
        )
    }
}
