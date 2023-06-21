package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.immutable.mapToImmutableList
import com.dzirbel.kotify.util.immutable.persistentListOfNotNull
import java.util.concurrent.TimeUnit

@Composable
fun AlbumPageHeader(presenter: AlbumPresenter, state: AlbumPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadedImage(url = state.album?.largestImage?.cached?.url)

            state.album?.let { album ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(album.name, style = MaterialTheme.typography.h5)

                    album.artists.cachedOrNull?.let { artists ->
                        LinkedText(
                            onClickLink = { artistId -> pageStack.mutate { to(ArtistPage(artistId = artistId)) } },
                        ) {
                            text("By ")
                            list(artists) { artist -> link(text = artist.name, link = artist.id.value) }
                        }
                    }

                    album.releaseDate?.let { Text(it) }

                    val totalDurationMins = state.totalDurationMs
                        ?.let { TimeUnit.MILLISECONDS.toMinutes(it) }
                        ?.let { ", $it mins" }

                    Text("${state.album.totalTracks} songs" + totalDurationMins)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToggleSaveButton(isSaved = state.isSaved, size = Dimens.iconMedium) {
                            presenter.emitAsync(AlbumPresenter.Event.ToggleSave(save = it))
                        }

                        PlayButton(context = Player.PlayContext.album(state.album))
                    }

                    AverageStarRating(ratings = state.trackRatings.values.mapToImmutableList { it.value })
                }
            }
        }

        if (state.album != null) {
            InvalidateButton(
                refreshing = state.refreshing,
                updated = state.albumUpdatedMs?.toEpochMilli(),
                updatedFormat = { "Album synced $it" },
                updatedFallback = "Album never synced",
                onClick = { presenter.emitAsync(AlbumPresenter.Event.Load(invalidate = true)) },
            )
        }
    }
}

@Composable
fun AlbumPageContent(presenter: AlbumPresenter, state: AlbumPresenter.ViewModel) {
    if (state.tracks.hasElements) {
        Table(
            columns = state.trackProperties,
            items = state.tracks,
            onSetSort = {
                presenter.emitAsync(AlbumPresenter.Event.SetSort(persistentListOfNotNull(it)))
            },
        )
    } else {
        PageLoadingSpinner()
    }
}
