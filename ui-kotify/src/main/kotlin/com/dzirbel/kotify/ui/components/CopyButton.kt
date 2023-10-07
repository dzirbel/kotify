package com.dzirbel.kotify.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.coroutines.Computation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_ICON_RESET = 3.seconds

/**
 * A simple icon button which sets [contents] as the clipboard contents on click and changes its icon to a check for
 * [iconResetMs] milliseconds before reverting.
 */
@Composable
fun CopyButton(
    contents: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dimens.iconMedium,
    iconResetMs: Duration = DEFAULT_ICON_RESET,
) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.Computation }
    var resetIconJob: Job? by remember { mutableStateOf(null) }

    IconButton(
        // override the to use the default in case this is used within a text field
        modifier = modifier.pointerHoverIcon(PointerIcon.Default),
        enabled = !copied,
        onClick = {
            if (setClipboard(contents)) {
                copied = true
                resetIconJob?.cancel()
                resetIconJob = scope.launch {
                    resetIconJob = null
                    delay(iconResetMs)
                    copied = false
                }
            }
        },
    ) {
        CachedIcon(name = if (copied) "task" else "content-copy", contentDescription = "Copy", size = iconSize)
    }
}
