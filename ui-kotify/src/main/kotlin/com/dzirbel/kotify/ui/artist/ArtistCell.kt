package com.dzirbel.kotify.ui.artist

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.ui.LocalSavedArtistRepository
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.star.AverageArtistRating
import com.dzirbel.kotify.ui.contextmenu.artistContextMenuItems
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun ArtistCell(
    artist: ArtistViewModel,
    imageSize: Dp,
    onClick: () -> Unit,
    onMiddleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContextMenuArea(items = { artistContextMenuItems(artist) }) {
        Column(
            modifier
                .instrument()
                .clickable(onClick = onClick)
                .onClick(matcher = PointerMatcher.mouse(PointerButton.Tertiary), onClick = onMiddleClick)
                .padding(Dimens.space3),
        ) {
            LoadedImage(artist, modifier = Modifier.align(Alignment.CenterHorizontally), size = imageSize)

            Spacer(Modifier.height(Dimens.space3))

            Row(
                modifier = Modifier.widthIn(max = Dimens.contentImage),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                Text(text = artist.name, modifier = Modifier.weight(1f))

                ToggleSaveButton(repository = LocalSavedArtistRepository.current, id = artist.id)

                PlayButton(context = Player.PlayContext.artist(artist), size = Dimens.iconSmall)
            }

            AverageArtistRating(artist.id)
        }
    }
}
