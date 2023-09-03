package com.dzirbel.kotify.ui.util

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Provides [ContentAlpha.disabled] for [LocalContentAlpha] when [disabled] is true, with no effect when [disabled] is
 * false.
 */
@Composable
fun ProvidingDisabledContentAlpha(disabled: Boolean = true, content: @Composable () -> Unit) {
    if (disabled) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled, content = content)
    } else {
        content()
    }
}
