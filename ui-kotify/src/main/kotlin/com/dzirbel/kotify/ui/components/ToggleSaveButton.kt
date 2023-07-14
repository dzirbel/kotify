package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository2.SavedRepository
import com.dzirbel.kotify.repository2.util.ToggleableState
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument

/**
 * A generic button to toggle to the saved status of a artist/album/track/etc.
 */
@Composable
fun ToggleSaveButton(
    isSaved: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconSmall,
    onSave: (Boolean) -> Unit,
) {
    val expectedState = remember(isSaved) { mutableStateOf(isSaved) }
    IconButton(
        modifier = modifier.instrument().size(size),
        enabled = isSaved != null && expectedState.value == isSaved,
        onClick = {
            if (isSaved != null) {
                expectedState.value = !isSaved
                onSave(!isSaved)
            }
        },
    ) {
        CachedIcon(
            name = if (isSaved == true) "favorite" else "favorite-border",
            size = size,
            contentDescription = "Save",
        )
    }
}

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
        )
    }
}
