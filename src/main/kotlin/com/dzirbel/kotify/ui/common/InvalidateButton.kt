package com.dzirbel.kotify.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun RefreshButton(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = Dimens.iconMedium,
    content: @Composable RowScope.() -> Unit
) {
    SimpleTextButton(
        modifier = modifier,
        enabled = !refreshing,
        onClick = onClick
    ) {
        content()

        if (refreshing) {
            CircularProgressIndicator(Modifier.size(iconSize))
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun InvalidateButton(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    updated: Long?,
    updatedFormat: (String) -> String = { "Last updated $it" },
    updatedFallback: String = "Never updated",
    iconSize: Dp = Dimens.iconMedium,
    onClick: () -> Unit,
) {
    RefreshButton(
        modifier = modifier,
        refreshing = refreshing,
        iconSize = iconSize,
        onClick = { onClick() }
    ) {
        Text(
            text = updated?.let {
                liveRelativeDateText(timestamp = updated, format = updatedFormat)
            } ?: updatedFallback
        )

        Spacer(Modifier.width(Dimens.space2))
    }
}
