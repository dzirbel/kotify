package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun VerticalScroll(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    includeScrollbarWhenUnused: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    OptionalScrollbarLayout(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        includeScrollbarWhenUnused = includeScrollbarWhenUnused,
    ) {
        Column(columnModifier.verticalScroll(scrollState), content = content)
    }
}

@Composable
fun LazyVerticalScroll(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    includeScrollbarWhenUnused: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    OptionalScrollbarLayout(
        adapter = rememberScrollbarAdapter(lazyListState),
        modifier = modifier,
        includeScrollbarWhenUnused = includeScrollbarWhenUnused,
    ) {
        LazyColumn(modifier = columnModifier, state = lazyListState, content = content)
    }
}

@Composable
private fun OptionalScrollbarLayout(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    includeScrollbarWhenUnused: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            check(measurables.size == 2)

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
            content()
            VerticalScrollbar(adapter = adapter)
        },
    )
}
