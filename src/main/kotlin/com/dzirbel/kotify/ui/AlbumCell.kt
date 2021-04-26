package com.dzirbel.kotify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.ui.common.LoadedImage
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate

@Composable
fun AlbumCell(
    album: Album,
    savedAlbums: Set<String>?,
    pageStack: MutableState<PageStack>,
    onToggleSave: (Boolean) -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(AlbumPage(albumId = album.id!!)) } }
            .padding(Dimens.space3)
    ) {
        LoadedImage(
            url = album.images.firstOrNull()?.url,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(Dimens.space2))

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            val isSaved = savedAlbums?.contains(album.id)
            ToggleSaveButton(isSaved = isSaved) { onToggleSave(it) }

            album.uri?.let { uri ->
                PlayButton(contextUri = uri, size = Dimens.iconSmall)
            }
        }
    }
}
