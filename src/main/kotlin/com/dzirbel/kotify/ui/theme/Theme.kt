package com.dzirbel.kotify.ui.theme

import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
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
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    LocalColors.current.withSurface(increment = 0, content = content)
                }
            }
        }
    }
}
