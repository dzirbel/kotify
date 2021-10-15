package com.dzirbel.kotify.ui.components.panel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Determines how the panel of a [SidePanel] is measured. It begins with [initialSize] but may be resized based on the
 * various min/max parameters.
 */
data class PanelSize(
    val initialSize: FixedOrPercent,

    val minPanelSizeDp: Dp? = null,
    val minPanelSizePercent: Float? = null,
    val maxPanelSizeDp: Dp? = null,
    val maxPanelSizePercent: Float? = null,

    val minContentSizeDp: Dp? = null,
    val minContentSizePercent: Float? = null,
    val maxContentSizeDp: Dp? = null,
    val maxContentSizePercent: Float? = null,
) {
    init {
        val hasMinPanelSize = minPanelSizeDp != null || minPanelSizePercent != null
        val hasMaxPanelSize = maxPanelSizeDp != null || maxPanelSizePercent != null
        val hasMinContentSize = minContentSizeDp != null || minContentSizePercent != null
        val hasMaxContentSize = maxContentSizeDp != null || maxContentSizePercent != null

        require(!(hasMinPanelSize && hasMaxContentSize)) {
            "cannot set both a minimum panel size and maximum content size: this could lead to cases where neither " +
                "the panel nor content can fill the view"
        }

        require(!(hasMaxPanelSize && hasMinContentSize)) {
            "cannot set both a maximum panel size and minimum content size: this could lead to cases where neither " +
                "the panel nor content can fill the view"
        }
    }

    /**
     * Determines the minimum panel size with the given [total] size, or zero if there is no minimum size.
     */
    fun minPanelSize(total: Dp): Dp {
        return listOfNotNull(
            minPanelSizeDp,
            minPanelSizePercent?.let { total * it },
            maxContentSizeDp?.let { total - it },
            maxContentSizePercent?.let { total - total * it },
        ).minOrNull() ?: 0.dp
    }

    /**
     * Determines the minimum panel size with the given [total] size, or [total] if there is no maximum size.
     */
    fun maxPanelSize(total: Dp): Dp {
        return listOfNotNull(
            maxPanelSizeDp,
            maxPanelSizePercent?.let { total * it },
            minContentSizeDp?.let { total - it },
            minContentSizePercent?.let { total - total * it },
        ).maxOrNull() ?: total
    }
}
