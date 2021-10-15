package com.dzirbel.kotify.ui.components.panel

import androidx.compose.ui.unit.Dp

/**
 * Wraps either a [FixedOrPercent.Fixed] [Dp] or a [FixedOrPercent.Percent] percentage.
 */
sealed class FixedOrPercent {
    abstract fun measure(total: Dp): Dp

    data class Fixed(val value: Dp) : FixedOrPercent() {
        override fun measure(total: Dp) = value
    }

    data class Percent(val value: Float) : FixedOrPercent() {
        override fun measure(total: Dp) = total * value
    }
}
