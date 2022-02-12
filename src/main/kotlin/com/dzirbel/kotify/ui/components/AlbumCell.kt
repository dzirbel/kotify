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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.mutate

@Composable
fun AlbumCell(
    album: Album,
    isSaved: Boolean?,
    onToggleSave: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(AlbumPage(albumId = album.id.value)) } }
            .padding(Dimens.space3),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        LoadedImage(
            url = album.largestImage.cached?.url,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            ToggleSaveButton(isSaved = isSaved) { onToggleSave(it) }

            PlayButton(context = Player.PlayContext.album(album), size = Dimens.iconSmall)
        }
    }
}

private const val SMALL_ALBUM_BUTTONS_BACKGROUND_ALPHA = 0.6f

@Composable
fun SmallAlbumCell(
    album: Album,
    isSaved: Boolean?,
    onToggleSave: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(AlbumPage(albumId = album.id.value)) } }
            .padding(Dimens.space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        Box {
            LoadedImage(
                url = album.largestImage.cached?.url,
                size = Dimens.contentImageSmall,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = LocalColors.current.surface2.copy(alpha = SMALL_ALBUM_BUTTONS_BACKGROUND_ALPHA),
                        shape = RoundedCornerShape(size = Dimens.cornerSize),
                    )
                    .padding(Dimens.space1),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                ToggleSaveButton(isSaved = isSaved) { onToggleSave(it) }

                PlayButton(context = Player.PlayContext.album(album), size = Dimens.iconSmall)
            }
        }

        Text(
            text = album.name,
            fontSize = Dimens.fontCaption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Dimens.contentImageSmall),
        )
    }
}
