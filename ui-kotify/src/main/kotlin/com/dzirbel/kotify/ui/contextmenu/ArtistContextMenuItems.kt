package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.setClipboard

fun artistContextMenuItems(artist: ArtistViewModel): List<ContextMenuItem> {
    return listOf(CopyArtistContextMenuItem(artist))
}

private class CopyArtistContextMenuItem(artist: ArtistViewModel) : AugmentedContextMenuItem(
    label = "Copy Artist ID",
    onClick = { setClipboard(artist.id) },
) {
    @Composable
    override fun StartIcon() {
        CachedIcon(name = "content-copy", size = Dimens.iconSmall)
    }
}
