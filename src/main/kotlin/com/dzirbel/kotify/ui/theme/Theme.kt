package com.dzirbel.kotify.ui.theme

import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object Theme {
    /**
     * Applies all theme elements ([colors], dimensions, etc) to [content].
     */
    @Composable
    fun apply(colors: Colors = Colors.current, content: @Composable () -> Unit) {
        Colors.current = colors
        colors.applyColors {
            Dimens.applyDimens {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    content()
                }
            }
        }
    }
}
