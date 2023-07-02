package com.dzirbel.kotify.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.setClipboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_ICON_RESET_MS = 5_000L

/**
 * A simple icon button which sets [contents] as the clipboard contents on click and changes its icon to a check for
 * [iconResetMs] milliseconds before reverting.
 */
@Composable
fun CopyButton(
    contents: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dimens.iconMedium,
    iconResetMs: Long = DEFAULT_ICON_RESET_MS,
) {
    val copied = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var resetIconJob: Job? = null

    IconButton(
        modifier = modifier,
        onClick = {
            if (setClipboard(contents)) {
                copied.value = true
                resetIconJob?.cancel()
                resetIconJob = scope.launch {
                    resetIconJob = null
                    delay(iconResetMs)
                    copied.value = false
                }
            }
        },
    ) {
        CachedIcon(name = if (copied.value) "task" else "content-copy", contentDescription = "Copy", size = iconSize)
    }
}
