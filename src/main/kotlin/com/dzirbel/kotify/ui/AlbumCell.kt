package com.dzirbel.kotify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate

@Composable
fun AlbumCell(
    album: Album,
    isSaved: Boolean?,
    pageStack: MutableState<PageStack>,
    onToggleSave: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .clickable { pageStack.mutate { to(AlbumPage(albumId = album.id.value)) } }
            .padding(Dimens.space3)
    ) {
        LoadedImage(
            url = album.images.cached.firstOrNull()?.url,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        VerticalSpacer(Dimens.space2)

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
