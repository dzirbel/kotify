package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A generic button to toggle to the saved status of a artist/album/track/etc.
 */
@Composable
fun ToggleSaveButton(
    isSaved: Boolean,
    size: Dp = Dimens.iconSmall,
    onSave: (Boolean) -> Unit
) {
    val expectedState = remember { mutableStateOf(isSaved) }
    IconButton(
        modifier = Modifier.size(size),
        enabled = expectedState.value == isSaved,
        onClick = {
            expectedState.value = !isSaved
            onSave(!isSaved)
        }
    ) {
        CachedIcon(
            name = if (isSaved) "favorite" else "favorite-border",
            size = size,
            contentDescription = "Save"
        )
    }
}
