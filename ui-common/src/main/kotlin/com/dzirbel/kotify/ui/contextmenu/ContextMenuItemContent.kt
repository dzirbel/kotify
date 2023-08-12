package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
internal fun ContextMenuItemContent(
    item: ContextMenuItem,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    menuOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    @Suppress("NamedArguments")
    when (item) {
        is AugmentedContextMenuItem -> AugmentedContextMenuItem(item, params, onDismissRequest, modifier)
        is CustomContentContextMenuItem -> CustomContentContextMenuItem(item, params, onDismissRequest, modifier)
        is GenericContextMenuItem -> GenericContextMenuItem(item, params, onDismissRequest, modifier)
        is ContextMenuGroup -> ContextMenuGroup(item, params, onDismissRequest, menuOpen, modifier)
        is ContextMenuDivider -> ContextMenuDivider(params, modifier)
        else -> DefaultContextMenuItem(item, params, onDismissRequest, modifier)
    }
}

@Composable
private fun AugmentedContextMenuItem(
    item: AugmentedContextMenuItem,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (item.enabled) LocalContentAlpha.current else ContentAlpha.disabled
    CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
        Row(
            modifier = modifier
                .itemSize(params)
                .clickable(enabled = item.enabled) {
                    onDismissRequest()
                    item.onClick()
                }
                .padding(params.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(params.iconPadding),
        ) {
            item.StartIcon()
            Text(item.label, modifier = Modifier.weight(1f))
            item.EndIcon()
        }
    }
}

@Composable
private fun CustomContentContextMenuItem(
    item: CustomContentContextMenuItem,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .itemSize(params)
            .clickable(enabled = item.clickable) {
                onDismissRequest()
                item.onClick()
            }
            .padding(params.padding),
        contentAlignment = Alignment.CenterStart,
    ) {
        item.Content()
    }
}

@Composable
private fun GenericContextMenuItem(
    item: GenericContextMenuItem,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    item.Content(onDismissRequest = onDismissRequest, params = params, modifier = modifier)
}

@Composable
private fun ContextMenuGroup(
    item: ContextMenuGroup,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    menuOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    // hacky, but works: re-uses the same enter interaction every time
    val enterInteraction = remember { HoverInteraction.Enter() }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(menuOpen) {
        interactionSource.emit(if (menuOpen) enterInteraction else HoverInteraction.Exit(enterInteraction))
    }

    Box(
        modifier = modifier
            .itemSize(params)
            // use clickable() to display hover indication (not provided by just hoverable()), but without any click
            // action; provide a custom interaction source which also adds a hover interaction when the menu is open
            .clickable(
                enabled = false,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {},
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            // apply padding to the inner row so that the position of the nested menu is correct
            modifier = Modifier.fillMaxWidth().padding(params.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.label)
            item.EndIcon()
        }

        if (menuOpen) {
            ContextMenuPopup(
                params = params,
                popupPositionProvider = rememberNestedDropdownPositionProvider(
                    windowMargin = params.windowMargin,
                ),
                onDismissRequest = onDismissRequest,
                items = item.items,
            )
        }
    }
}

@Composable
private fun ContextMenuDivider(params: ContextMenuParams, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier.itemWidth(params).background(params.dividerColor),
        content = {},
        measurePolicy = { _, constraints ->
            layout(width = constraints.maxWidth, height = params.dividerHeight.roundToPx()) {}
        },
    )
}

@Composable
private fun DefaultContextMenuItem(
    item: ContextMenuItem,
    params: ContextMenuParams,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .itemSize(params)
            .clickable {
                onDismissRequest()
                item.onClick()
            }
            .padding(params.padding),
        text = item.label,
    )
}

private fun Modifier.itemWidth(params: ContextMenuParams): Modifier {
    return fillMaxWidth().widthIn(min = params.minWidth, max = params.maxWidth)
}

private fun Modifier.itemHeight(params: ContextMenuParams) = heightIn(min = params.itemMinHeight)

private fun Modifier.itemSize(params: ContextMenuParams) = itemWidth(params).itemHeight(params)
