package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull

@Composable
fun AlbumCell(
    album: AlbumViewModel,
    isSaved: Boolean?,
    onToggleSave: (Boolean) -> Unit,
    showRating: Boolean = false,
    averageRating: AverageRating? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .instrument()
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable(onClick = onClick)
            .padding(Dimens.space3),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        LoadedImage(album.largestImageUrl, modifier = Modifier.align(Alignment.CenterHorizontally))

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            ToggleSaveButton(isSaved = isSaved) { onToggleSave(it) }

            PlayButton(context = Player.PlayContext.album(album), size = Dimens.iconSmall)
        }

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                album.albumType?.let { albumType ->
                    CachedIcon(
                        name = albumType.iconName,
                        size = Dimens.iconSmall,
                        contentDescription = albumType.displayName,
                    )
                }

                album.parsedReleaseDate?.let { releaseDate ->
                    Text(text = releaseDate.year.toString())
                }

                if (album.parsedReleaseDate != null && album.totalTracks != null) {
                    Interpunct()
                }

                album.totalTracks?.let { totalTracks ->
                    Text("$totalTracks tracks")
                }
            }
        }

        if (showRating) {
            AverageStarRating(averageRating = averageRating)
        }
    }
}

@Composable
fun AlbumCell(album: AlbumViewModel, showRating: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .instrument()
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable(onClick = onClick)
            .padding(Dimens.space3),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        LoadedImage(album.largestImageUrl, modifier = Modifier.align(Alignment.CenterHorizontally))

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            ToggleSaveButton(repository = SavedAlbumRepository, id = album.id)

            PlayButton(context = Player.PlayContext.album(album), size = Dimens.iconSmall)
        }

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                album.albumType?.let { albumType ->
                    CachedIcon(
                        name = albumType.iconName,
                        size = Dimens.iconSmall,
                        contentDescription = albumType.displayName,
                    )
                }

                album.parsedReleaseDate?.let { releaseDate ->
                    Text(text = releaseDate.year.toString())
                }

                if (album.parsedReleaseDate != null && album.totalTracks != null) {
                    Interpunct()
                }

                album.totalTracks?.let { totalTracks ->
                    Text("$totalTracks tracks")
                }
            }
        }

        if (showRating) {
            val averageRating = remember(album.id) {
                AlbumTracksRepository.stateOf(id = album.id)
                    .mapNotNull { it?.cachedValue?.map { track -> track.id } }
                    .flatMapLatest { tracks -> TrackRatingRepository.averageRatingStateOf(ids = tracks) }
            }
                .collectAsState(initial = null)
                .value

            AverageStarRating(averageRating = averageRating)
        }
    }
}

@Composable
fun SmallAlbumCell(album: AlbumViewModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable(onClick = onClick)
            .padding(Dimens.space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        Box {
            LoadedImage(
                // TODO use thumbnail image instead
                urlFlow = album.largestImageUrl,
                size = Dimens.contentImageSmall,
                modifier = Modifier.align(Alignment.Center),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = LocalColors.current.overlay,
                        shape = RoundedCornerShape(size = Dimens.cornerSize),
                    )
                    .padding(Dimens.space1),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                ToggleSaveButton(repository = SavedAlbumRepository, id = album.id)

                PlayButton(context = Player.PlayContext.album(album), size = Dimens.iconSmall)
            }
        }

        Text(
            text = album.name,
            style = MaterialTheme.typography.overline,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Dimens.contentImageSmall),
        )
    }
}
