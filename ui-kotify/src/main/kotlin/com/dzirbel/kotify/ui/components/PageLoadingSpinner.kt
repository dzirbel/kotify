package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
fun ColumnScope.PageLoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            Modifier
                .padding(vertical = Dimens.space5)
                .size(Dimens.iconLarge),
        )
    }
}
