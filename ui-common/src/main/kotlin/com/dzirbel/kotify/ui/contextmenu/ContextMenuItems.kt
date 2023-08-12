package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A [ContextMenuItemContent] which may also optionally provide start and/or end icons and an [enabled] state.
 */
open class AugmentedContextMenuItem(
    label: String,
    onClick: () -> Unit,
    val enabled: Boolean = true,
) : ContextMenuItem(label = label, onClick = onClick) {
    @Composable
    open fun StartIcon() {}

    @Composable
    open fun EndIcon() {}
}

/**
 * A [ContextMenuItemContent] which provides its own Composable [Content].
 *
 * Standard styling is applied to the item, including padding, sizing, and clickable behavior (enabled if [clickable] is
 * true). For complete control over the item, use [GenericContextMenuItem].
 */
abstract class CustomContentContextMenuItem(onClick: () -> Unit) : ContextMenuItem(label = "", onClick = onClick) {
    open val clickable: Boolean = true

    @Composable
    abstract fun Content()
}

/**
 * A [ContextMenuItemContent] which provides its own Composable [Content] and has no generic styling applied to it.
 */
abstract class GenericContextMenuItem : ContextMenuItem(
    label = "",
    onClick = { error("generic item should not be clickable") },
) {
    /**
     * The Composable content of this item.
     *
     * @param onDismissRequest callback which may be invoked to close the context menu; provided for items which
     *  implement their own click handling (to close the menu on click)
     * @param params [ContextMenuParams] provided to the context menu (to be optionally used for padding, etc)
     * @param modifier [Modifier] which must be applied to the root element (in order for hover states of nested menus
     *  to work properly)
     */
    @Composable
    abstract fun Content(onDismissRequest: () -> Unit, params: ContextMenuParams, modifier: Modifier)
}

/**
 * A [ContextMenuItemContent] which displays a nested dropdown provided by [items].
 */
open class ContextMenuGroup(
    label: String,
    val items: () -> List<ContextMenuItem>,
) : ContextMenuItem(label = label, onClick = { error("group should not be clickable") }) {
    /**
     * The icon displayed at the end of the item; by default an arrow pointing to the right.
     */
    @Composable
    open fun EndIcon() {
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Expand")
    }
}

/**
 * A [ContextMenuItemContent] which displays a simple divider between items.
 */
object ContextMenuDivider : ContextMenuItem(
    label = "",
    onClick = { error("divider should not be clickable") },
)
