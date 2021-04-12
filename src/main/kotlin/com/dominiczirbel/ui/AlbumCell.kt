package com.dominiczirbel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.svgResource
import com.dominiczirbel.network.model.Album
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate

@Composable
fun AlbumCell(album: Album, pageStack: MutableState<PageStack>) {
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
            Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = album.name, modifier = Modifier.weight(1f))

            album.uri?.let { uri ->
                IconButton(
                    enabled = Player.playable,
                    modifier = Modifier.size(Dimens.iconSmall),
                    onClick = { Player.play(contextUri = uri) }
                ) {
                    val playing = Player.playbackContext.value?.uri == uri
                    Icon(
                        painter = svgResource("play-circle-outline.svg"),
                        modifier = Modifier.size(Dimens.iconSmall),
                        contentDescription = "Play",
                        tint = Colors.current.highlighted(highlight = playing)
                    )
                }
            }
        }
    }
}
