package com.dzirbel.kotify.ui.theme

import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dzirbel.kotify.Settings

object Theme {
    /**
     * Applies all theme elements ([colors], dimensions, etc) to [content].
     */
    @Composable
    fun apply(colors: Colors = Settings.colors, content: @Composable () -> Unit) {
        colors.applyColors {
            Dimens.applyDimens {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    content()
                }
            }
        }
    }
}
