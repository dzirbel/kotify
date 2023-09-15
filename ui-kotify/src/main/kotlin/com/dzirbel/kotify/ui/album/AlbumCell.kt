package com.dzirbel.kotify.ui.album

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.LocalSavedAlbumRepository
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun AlbumCell(album: AlbumViewModel, showRating: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .instrument()
            .clickable(onClick = onClick)
            .padding(Dimens.space3),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        LoadedImage(modifier = Modifier.align(Alignment.CenterHorizontally), key = album.id) { size ->
            album.imageUrlFor(size)
        }

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            ToggleSaveButton(repository = LocalSavedAlbumRepository.current, id = album.id)

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
            val scope = rememberCoroutineScope()
            val ratingRepository = LocalRatingRepository.current
            val averageRating = remember(album.id) {
                ratingRepository.averageRatingStateOfAlbum(albumId = album.id, scope = scope)
            }
                .collectAsState()
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
                modifier = Modifier.align(Alignment.Center),
                size = Dimens.contentImageSmall,
                key = album.id,
            ) { size -> album.imageUrlFor(size) }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = MaterialTheme.colors.background.copy(alpha = KotifyColors.current.overlayAlpha),
                        shape = RoundedCornerShape(size = Dimens.cornerSize),
                    )
                    .padding(Dimens.space1),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                ToggleSaveButton(repository = LocalSavedAlbumRepository.current, id = album.id)

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
