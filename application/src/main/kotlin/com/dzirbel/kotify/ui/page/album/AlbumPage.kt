package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.page.Page
import com.dzirbel.kotify.ui.page.PageScope
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.rememberRatingStates
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.coroutines.onEachIn
import com.dzirbel.kotify.util.immutable.persistentListOfNotNull
import com.dzirbel.kotify.util.takingIf
import com.dzirbel.kotify.util.time.formatMediumDuration
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration.Companion.milliseconds

data class AlbumPage(val albumId: String) : Page {
    @Composable
    override fun PageScope.bind() {
        val album = AlbumRepository.stateOf(id = albumId).collectAsState().value?.cachedValue

        val tracksAdapterState = rememberListAdapterState(
            key = albumId,
            defaultSort = TrackAlbumIndexProperty,
        ) { scope ->
            // TODO load full track objects
            AlbumTracksRepository.stateOf(id = albumId)
                .mapIn(scope) { it?.cachedValue }
                .onEachIn(scope) { tracks ->
                    tracks?.requestBatched(transactionName = { "album $albumId $it track artists" }) { it.artists }
                }
        }

        TrackRatingRepository.rememberRatingStates(tracksAdapterState.value) { it.id }

        val trackProperties: PersistentList<Column<TrackViewModel>> = remember(album) {
            persistentListOf(
                TrackPlayingColumn(
                    trackIdOf = { it.id },
                    playContextFromTrack = { track ->
                        album?.let {
                            Player.PlayContext.albumTrack(album = album, index = track.trackNumber)
                        }
                    },
                ),
                TrackAlbumIndexProperty,
                TrackSavedProperty(trackIdOf = { track -> track.id }),
                TrackNameProperty,
                TrackArtistsProperty,
                TrackRatingProperty(trackIdOf = { track -> track.id }),
                TrackDurationProperty,
                TrackPopularityProperty,
            )
        }

        DisplayVerticalScrollPage(
            title = album?.name,
            header = {
                AlbumHeader(albumId = albumId, adapter = tracksAdapterState)
            },
        ) {
            if (tracksAdapterState.derived { it.hasElements }.value) {
                Table(
                    columns = trackProperties,
                    items = tracksAdapterState.value,
                    onSetSort = { tracksAdapterState.withSort(persistentListOfNotNull(it)) },
                )
            } else {
                PageLoadingSpinner()
            }
        }
    }
}

@Composable
private fun AlbumHeader(albumId: String, adapter: ListAdapterState<TrackViewModel>) {
    val albumCacheState = AlbumRepository.stateOf(albumId).collectAsState().value
    val album = albumCacheState?.cachedValue

    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadedImage(key = album?.id) { size -> album?.imageUrlFor(size) }

            if (album != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(album.name, style = MaterialTheme.typography.h5)

                    album.artists.collectAsState().value?.let { artists ->
                        LinkedText(
                            onClickLink = { artistId -> pageStack.mutate { to(ArtistPage(artistId = artistId)) } },
                        ) {
                            text("By ")
                            list(artists) { artist -> link(text = artist.name, link = artist.id) }
                        }
                    }

                    album.releaseDate?.let { Text(it) }

                    val totalDuration = remember(adapter.value) {
                        takingIf(adapter.value.hasElements) {
                            adapter.value.sumOf { it.durationMs }.milliseconds.formatMediumDuration()
                        }
                    }

                    Text("${album.totalTracks} songs" + totalDuration?.let { ", $it" })

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToggleSaveButton(repository = SavedAlbumRepository, id = albumId)

                        PlayButton(context = Player.PlayContext.album(album))
                    }

                    val averageRating = remember(albumId) {
                        AlbumTracksRepository.stateOf(id = albumId)
                            .mapNotNull { it?.cachedValue?.map { track -> track.id } }
                            .flatMapLatest { tracks -> TrackRatingRepository.averageRatingStateOf(ids = tracks) }
                    }
                        .collectAsState(initial = null)
                        .value

                    AverageStarRating(averageRating = averageRating)
                }
            }
        }

        Column {
            InvalidateButton(repository = AlbumRepository, id = albumId, entityName = "Album")
            InvalidateButton(repository = AlbumTracksRepository, id = albumId, entityName = "Tracks")
        }
    }
}
