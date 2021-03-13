package com.dominiczirbel.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val LOADING_STROKE_WIDTH = 1.dp
private val LOADING_SIZE = 8.dp

@Composable
fun LoadingButton(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    loading: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        enabled = enabled && !loading,
        modifier = modifier.animateContentSize(),
        colors = if (loading) {
            // when loading, preserve the colors as if still enabled
            ButtonDefaults.buttonColors(
                disabledBackgroundColor = MaterialTheme.colors.primary,
                disabledContentColor = contentColorFor(MaterialTheme.colors.primary)
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        onClick = onClick
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier.size(LOADING_SIZE),
                strokeWidth = LOADING_STROKE_WIDTH,
            )
        } else {
            content()
        }
    }
}
