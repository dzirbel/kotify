package com.dzirbel.kotify.ui

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.dzirbel.contextmenu.ContextMenuMeasurements
import com.dzirbel.contextmenu.MaterialContextMenuRepresentation
import com.dzirbel.contextmenu.MaterialTextContextMenu
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
                    LocalContextMenuRepresentation provides MaterialContextMenuRepresentation(
                        measurements = ContextMenuMeasurements(
                            menuTopPadding = 0.dp,
                            menuBottomPadding = 0.dp,
                            iconSize = Dimens.iconSmall,
                            popupShape = RoundedCornerShape(Dimens.cornerSize),
                            itemPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
                            dividerHeight = 1.dp,
                        ),
                    ),
                    LocalTextContextMenu provides MaterialTextContextMenu,
                    content = content,
                )
            }
        }
    }
}
