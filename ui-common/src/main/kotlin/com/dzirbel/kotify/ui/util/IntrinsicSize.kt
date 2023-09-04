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

/**
 * A [Modifier] which specifies the maximum intrinsic width of the wrapped element.
 *
 * This can be useful when specifying a parent width as the maximum intrinsic width of its children, but excluding some
 * of them.
 */
fun Modifier.maxIntrinsicWidth(width: Dp): Modifier {
    return then(
        object : LayoutModifier {
            override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
                return width.roundToPx()
            }
        },
    )
}
