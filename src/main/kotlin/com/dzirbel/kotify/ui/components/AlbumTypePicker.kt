package com.dzirbel.kotify.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.immutable.toImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet

@Composable
fun AlbumTypePicker(
    albumTypeCounts: ImmutableMap<SpotifyAlbum.Type?, Int>?,
    albumTypes: PersistentSet<SpotifyAlbum.Type>,
    onSelectAlbumTypes: (PersistentSet<SpotifyAlbum.Type>) -> Unit,
) {
    ToggleButtonGroup(
        elements = SpotifyAlbum.Type.values().toImmutableList(),
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
