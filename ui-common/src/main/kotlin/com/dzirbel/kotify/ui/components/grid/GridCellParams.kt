package com.dzirbel.kotify.ui.components.grid

import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

data class GridCellParams(
    val backgroundSurfaceIncrement: Int,
    val cornerSize: Dp = Dimens.cornerSize,
)
