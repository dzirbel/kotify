package com.dzirbel.kotify.ui.components.grid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Dimens

data class GridCellParams(
    val elevation: Dp = 0.dp,
    val cornerSize: Dp = Dimens.cornerSize,
)
