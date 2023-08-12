package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

data class ContextMenuParams(
    val minWidth: Dp = Dimens.contextMenuMinWidth,
    val maxWidth: Dp = Dimens.contextMenuMaxWidth,
    val itemMinHeight: Dp = Dimens.contextMenuItemHeight,
    val padding: PaddingValues = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
    val iconPadding: Dp = Dimens.space2,
    val elevation: Dp = Dimens.contextMenuElevation,
    val windowMargin: Dp = Dimens.space3,
    val popupShape: Shape = RoundedCornerShape(Dimens.cornerSize),
    val dividerColor: Color,
    val dividerHeight: Dp = Dimens.divider,
    val backgroundColor: Color,
)

// TODO height of dropdowns that take up the entire window is a bit too much
// TODO clicking a menu group item closes the dropdown
class AugmentedContextMenuRepresentation(
    colors: Colors,
    private val params: ContextMenuParams = ContextMenuParams(
        dividerColor = colors.dividerColor,
        backgroundColor = colors.popupBackground,
    ),
) : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status is ContextMenuState.Status.Open) {
            ContextMenuPopup(
                params = params,
                popupPositionProvider = rememberPopupPositionProviderAtPosition(
                    positionPx = status.rect.topLeft, // rect is 0x0 at the mouse position
                    windowMargin = params.windowMargin,
                ),
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
                items = items,
            )
        }
    }
}
