package com.dzirbel.kotify.ui.theme

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.dzirbel.contextmenu.ContextMenuMeasurements
import com.dzirbel.contextmenu.MaterialContextMenuRepresentation
import com.dzirbel.contextmenu.MaterialTextContextMenu
import com.dzirbel.kotify.ui.util.instrumentation.LocalInstrumentationCompositionHighlightEnabled
import com.dzirbel.kotify.ui.util.instrumentation.LocalInstrumentationMetricsPanelsEnabled

object KotifyTheme {
    @Composable
    fun Apply(
        colors: KotifyColors,
        instrumentationHighlightCompositions: Boolean = false,
        instrumentationMetricsPanels: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        colors.Apply {
            MaterialTheme(typography = Typography(defaultFontFamily = KotifyTypography.Default)) {
                Dimens.ApplyDimens {
                    CompositionLocalProvider(
                        LocalInstrumentationCompositionHighlightEnabled provides instrumentationHighlightCompositions,
                        LocalInstrumentationMetricsPanelsEnabled provides instrumentationMetricsPanels,
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
}
