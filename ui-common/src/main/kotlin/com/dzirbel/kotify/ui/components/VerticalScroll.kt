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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Adds a simple vertical scrollbar to [content], which is placed in a [Box] with a [VerticalScrollbar] to the right.
 */
@Composable
fun VerticalScroll(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(0),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(columnModifier.verticalScroll(scrollState)) {
            content()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}
