package com.dzirbel.kotify.ui.components.table

import androidx.compose.ui.unit.Dp

/**
 * Determines how to measure the width of a column in a [Table].
 */
sealed class ColumnWidth {
    /**
     * A fixed-width column with the given [width].
     */
    class Fixed(val width: Dp) : ColumnWidth()

    /**
     * A column whose contents fill the maximum space, between optional [minWidth] and [maxWidth]. The width of the
     * column will be the maximum of the width of the cells in the column (including the header).
     */
    class Fill(val minWidth: Dp = Dp.Unspecified, val maxWidth: Dp = Dp.Unspecified) : ColumnWidth()

    /**
     * A column whose width is weighted among the space remaining after [Fixed] and [Fill] columns are allocated. The
     * remaining width is split between the weighted columns by their [weight].
     */
    class Weighted(val weight: Float) : ColumnWidth()

    /**
     * A column whose header determines the width of the column.
     */
    object MatchHeader : ColumnWidth()
}
