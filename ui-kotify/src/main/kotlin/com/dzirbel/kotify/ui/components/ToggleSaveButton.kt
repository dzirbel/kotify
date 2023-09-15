package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument

/**
 * A generic button to toggle the saved state of the entity with the given [id] via the given [repository].
 */
@Composable
fun ToggleSaveButton(
    repository: SavedRepository,
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconSmall,
) {
    val savedState = repository.savedStateOf(id = id).collectAsState().value
    IconButton(
        modifier = modifier.instrument().size(size),
        enabled = savedState is ToggleableState.Set,
        onClick = {
            check(savedState is ToggleableState.Set)
            repository.setSaved(id = id, saved = !savedState.value)
        },
    ) {
        CachedIcon(
            name = if (savedState?.value == true) "favorite" else "favorite-border",
            size = size,
            contentDescription = "Save",
            tint = KotifyColors.highlighted(savedState?.value == true),
        )
    }
}
