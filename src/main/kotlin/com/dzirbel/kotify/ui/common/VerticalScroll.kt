package com.dzirbel.kotify.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Adds a simple vertical scrollbar to [content], which is placed in a [Column] with a [VerticalScrollbar] to the right.
 */
@Composable
fun VerticalScroll(scrollState: ScrollState = rememberScrollState(0), content: @Composable ColumnScope.() -> Unit) {
    Row {
        Column(Modifier.verticalScroll(scrollState).weight(1f)) {
            content()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterVertically),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}
