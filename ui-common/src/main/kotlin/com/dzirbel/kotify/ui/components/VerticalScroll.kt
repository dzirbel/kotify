package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

/**
 * Adds a simple vertical scrollbar to [content], which is placed in a [Box] with a [VerticalScrollbar] to the right.
 */
@Composable
fun VerticalScroll(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    includeScrollbarWhenUnused: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val adapter = rememberScrollbarAdapter(scrollState)
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val contentMeasurable = measurables[0]
            val needScrollbar = includeScrollbarWhenUnused || adapter.contentSize > adapter.viewportSize

            if (needScrollbar) {
                val scrollbarMeasurable = measurables[1]
                val scrollbarPlaceable = scrollbarMeasurable.measure(constraints)
                val contentPlaceable = contentMeasurable.measure(
                    constraints.copy(maxWidth = constraints.maxWidth - scrollbarPlaceable.width),
                )

                layout(contentPlaceable.width + scrollbarPlaceable.width, contentPlaceable.height) {
                    contentPlaceable.place(0, 0)
                    scrollbarPlaceable.place(contentPlaceable.width, 0)
                }
            } else {
                val contentPlaceable = contentMeasurable.measure(constraints)

                layout(contentPlaceable.width, contentPlaceable.height) {
                    contentPlaceable.place(0, 0)
                }
            }
        },
        content = {
            Column(columnModifier.verticalScroll(scrollState), content = content)
            VerticalScrollbar(adapter = adapter)
        },
    )
}
