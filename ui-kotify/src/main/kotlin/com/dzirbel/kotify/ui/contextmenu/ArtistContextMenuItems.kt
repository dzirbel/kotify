package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.foundation.ContextMenuItem
import com.dzirbel.contextmenu.MaterialContextMenuItem
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.setClipboard

fun artistContextMenuItems(artist: ArtistViewModel): List<ContextMenuItem> {
    return listOf(
        MaterialContextMenuItem(
            label = "Copy Artist ID",
            onClick = { setClipboard(artist.id) },
            leadingIcon = { CachedIcon(name = "content-copy", size = Dimens.iconSmall) },
        ),
    )
}
