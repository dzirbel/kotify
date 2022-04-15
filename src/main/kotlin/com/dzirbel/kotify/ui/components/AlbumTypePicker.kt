package com.dzirbel.kotify.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun AlbumTypePicker(
    albumTypeCounts: Map<SpotifyAlbum.Type?, Int>?,
    albumTypes: Set<SpotifyAlbum.Type>,
    onSelectAlbumTypes: (Set<SpotifyAlbum.Type>) -> Unit,
) {
    ToggleButtonGroup(
        elements = SpotifyAlbum.Type.values().toList(),
        selectedElements = albumTypes,
        onSelectElements = onSelectAlbumTypes,
        content = { albumType ->
            CachedIcon(name = albumType.iconName, size = Dimens.iconSmall)

            HorizontalSpacer(width = Dimens.space2)

            val count = albumTypeCounts?.let { it[albumType] ?: 0 }
            if (count == null) {
                Text(albumType.displayName)
            } else {
                Text("${albumType.displayName} ($count)")
            }
        },
    )
}
