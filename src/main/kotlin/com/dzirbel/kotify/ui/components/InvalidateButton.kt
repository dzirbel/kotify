package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument

/**
 * Either an indefinite progress indicator when [refreshing] is true, otherwise a refresh icon.
 */
@Composable
fun RefreshIcon(refreshing: Boolean, size: Dp = Dimens.iconMedium) {
    if (refreshing) {
        CircularProgressIndicator(Modifier.size(size))
    } else {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh",
            modifier = Modifier.size(size),
        )
    }
}

/**
 * A [SimpleTextButton] which handles the common case of invalidating a cached resource.
 */
@Composable
fun InvalidateButton(
    refreshing: Boolean,
    updated: Long?,
    modifier: Modifier = Modifier,
    updatedFormat: (String) -> String = { "Synced $it" },
    updatedFallback: String = "Never synced",
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    onClick: () -> Unit,
) {
    SimpleTextButton(
        modifier = modifier.instrument(),
        enabled = !refreshing,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        Text(
            fontSize = fontSize,
            text = updated?.let {
                liveRelativeDateText(timestamp = updated, format = updatedFormat)
            } ?: updatedFallback,
        )

        HorizontalSpacer(Dimens.space2)

        RefreshIcon(refreshing = refreshing, size = Dimens.iconSizeFor(fontSize))
    }
}
