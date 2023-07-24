package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.VerticalScrollPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
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
import com.dzirbel.kotify.util.formatMediumDuration
import com.dzirbel.kotify.util.immutable.persistentListOfNotNull
import com.dzirbel.kotify.util.mapIn
import com.dzirbel.kotify.util.produceTransactionState
import com.dzirbel.kotify.util.takingIf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration.Companion.milliseconds

data class AlbumPage(val albumId: String) : Page<String?>() {
    @Composable
    override fun BoxScope.bind(visible: Boolean): String? {
        val album = AlbumRepository.stateOf(id = albumId).collectAsState().value?.cachedValue

        val tracksAdapterState = rememberListAdapterState(
            key = albumId,
            defaultSort = TrackAlbumIndexProperty,
        ) { scope ->
            // TODO load full track objects
            AlbumTracksRepository.stateOf(id = albumId).mapIn(scope) { it?.cachedValue }
        }

        TrackRatingRepository.rememberRatingStates(tracksAdapterState.value) { it.id.value }

        val trackProperties = remember(album) {
            persistentListOf(
                TrackPlayingColumn(
                    trackIdOf = { it.id.value },
                    playContextFromTrack = { track ->
                        album?.let {
                            Player.PlayContext.albumTrack(album = album, index = track.trackNumber)
                        }
                    },
                ),
                TrackAlbumIndexProperty,
                TrackSavedProperty(trackIdOf = { track -> track.id.value }),
                TrackNameProperty,
                // TrackArtistsProperty, TODO re-add
                TrackRatingProperty(trackIdOf = { track -> track.id.value }),
                TrackDurationProperty,
                TrackPopularityProperty,
            )
        }

        VerticalScrollPage(
            visible = visible,
            onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
            header = {
                AlbumHeader(albumId = albumId, adapter = tracksAdapterState)
            },
            content = {
                if (tracksAdapterState.derived { it.hasElements }.value) {
                    Table(
                        columns = trackProperties,
                        items = tracksAdapterState.value,
                        onSetSort = { tracksAdapterState.withSort(persistentListOfNotNull(it)) },
                    )
                } else {
                    PageLoadingSpinner()
                }
            },
        )

        return album?.name
    }

    override fun titleFor(data: String?) = data
}

@Composable
private fun AlbumHeader(albumId: String, adapter: ListAdapterState<Track>) {
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
            LoadedImage(album?.largestImage)

            if (album != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(album.name, style = MaterialTheme.typography.h5)

                    val artists = album.produceTransactionState("load album artists") { album.artists.live }.value
                    if (artists != null) {
                        LinkedText(
                            onClickLink = { artistId -> pageStack.mutate { to(ArtistPage(artistId = artistId)) } },
                        ) {
                            text("By ")
                            list(artists) { artist -> link(text = artist.name, link = artist.id.value) }
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
                            .mapNotNull { it?.cachedValue?.map { track -> track.id.value } }
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
