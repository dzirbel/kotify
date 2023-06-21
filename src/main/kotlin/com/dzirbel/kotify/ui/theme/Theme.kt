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
    fun Apply(colors: Colors = Settings.colors, content: @Composable () -> Unit) {
        colors.ApplyColors {
            Dimens.ApplyDimens {
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    LocalColors.current.WithSurface(increment = 0, content = content)
                }
            }
        }
    }
}
