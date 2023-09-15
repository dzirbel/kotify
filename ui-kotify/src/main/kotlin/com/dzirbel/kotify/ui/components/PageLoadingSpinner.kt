package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * Standard indeterminate loading element which occupies the maximum width, typically shown when the main content of a
 * page is being loaded.
 */
@Composable
fun PageLoadingSpinner() {
    // TODO center vertically; this is difficult since this is typically placed inside a scrolling column
    Box(Modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            Modifier
                .padding(vertical = Dimens.space5)
                .size(Dimens.iconLarge)
                .align(Alignment.Center),
        )
    }
}
