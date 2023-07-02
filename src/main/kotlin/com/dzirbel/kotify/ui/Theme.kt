package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.LocalInstrumentationCompositionHighlightEnabled
import com.dzirbel.kotify.ui.util.instrumentation.LocalInstrumentationMetricsPanelsEnabled

object Theme {
    /**
     * Applies all theme elements ([colors], dimensions, etc) to [content].
     */
    @Composable
    fun Apply(colors: Colors = Settings.colors, content: @Composable () -> Unit) {
        colors.ApplyColors {
            Dimens.ApplyDimens {
                CompositionLocalProvider(
                    LocalInstrumentationCompositionHighlightEnabled provides
                        Settings.instrumentationHighlightCompositions,
                    LocalInstrumentationMetricsPanelsEnabled provides Settings.instrumentationMetricsPanels,
                    content = content,
                )
            }
        }
    }
}
