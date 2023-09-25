package com.dzirbel.kotify.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

/**
 * A [Modifier] which provides manual intrinsic minimum and maximum sizes for the wrapped element.
 *
 * This can be useful when specifying a parent width as the maximum intrinsic width of its children, but excluding some
 * of them.
 */
fun Modifier.intrinsicSize(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
): Modifier {
    return then(
        object : LayoutModifier {
            override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
                return if (minWidth.isSpecified) {
                    minWidth.roundToPx()
                } else {
                    measurable.minIntrinsicWidth(height)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
                return if (maxWidth.isSpecified) {
                    maxWidth.roundToPx()
                } else {
                    measurable.maxIntrinsicWidth(height)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
                return if (minHeight.isSpecified) {
                    minHeight.roundToPx()
                } else {
                    measurable.minIntrinsicHeight(width)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
                return if (maxHeight.isSpecified) {
                    maxHeight.roundToPx()
                } else {
                    measurable.maxIntrinsicHeight(width)
                }
            }
        },
    )
}
