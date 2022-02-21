package com.dzirbel.kotify.ui.components.grid

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

// TODO ideally division logic would be part of Grid to avoid recompositioning the cells and everything else if the
//  divisions change or are added/removed
@Composable
fun <E> GridWithDivisions(
    elements: GridElements.DividedList<E>,
    selectedElement: E? = null,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Dimens.space2,
    verticalSpacing: Dp = Dimens.space3,
    cellAlignment: Alignment = Alignment.TopCenter,
    columns: Int? = null,
    detailInsertBackground: Color = LocalColors.current.surface2,
    detailInsertBorder: Color = LocalColors.current.dividerColor,
    detailInsertCornerSize: Dp = Dimens.cornerSize * 2,
    detailInsertAnimationDurationMs: Int = AnimationConstants.DefaultDurationMillis,
    detailInsertContent: @Composable ((E) -> Unit)? = null,
    cellContent: @Composable (element: E) -> Unit,
) {
    val divisions = elements.divisions
    val divider = elements.divider

    if (divisions.isEmpty()) return

    Column {
        for (division in divisions) {
            divider.headerContent(division)

            Grid(
                elements = division.elements,
                selectedElement = selectedElement,
                modifier = modifier,
                horizontalSpacing = horizontalSpacing,
                verticalSpacing = verticalSpacing,
                cellAlignment = cellAlignment,
                columns = columns,
                detailInsertBackground = detailInsertBackground,
                detailInsertBorder = detailInsertBorder,
                detailInsertCornerSize = detailInsertCornerSize,
                detailInsertAnimationDurationMs = detailInsertAnimationDurationMs,
                detailInsertContent = detailInsertContent,
                cellContent = cellContent,
            )
        }
    }
}
