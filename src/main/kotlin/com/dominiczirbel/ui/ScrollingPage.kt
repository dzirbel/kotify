package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.common.VerticalScroll
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState

private val LOADING_INDICATOR_SIZE = 60.dp
private val ERROR_ICON_SIZE = 100.dp

/**
 * Convenience wrapper for the common case where a [RemoteState] should be contained in a vertical scrolling box, and
 * handles loading/error states.
 */
@Composable
fun <T : Any> BoxScope.ScrollingPage(state: RemoteState<T>, content: @Composable (T) -> Unit) {
    when (state) {
        is RemoteState.Loading ->
            CircularProgressIndicator(Modifier.size(LOADING_INDICATOR_SIZE).align(Alignment.Center))

        is RemoteState.Error ->
            Column(Modifier.align(Alignment.Center)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    modifier = Modifier.size(ERROR_ICON_SIZE).align(Alignment.CenterHorizontally),
                    tint = Colors.current.error
                )

                Text(
                    text = "Encountered an error: ${state.throwable.message}",
                    color = Colors.current.error,
                    fontSize = Dimens.fontTitle
                )

                Text(
                    text = state.throwable.stackTraceToString(),
                    color = Colors.current.error,
                    fontFamily = FontFamily.Monospace
                )
            }

        is RemoteState.Success ->
            VerticalScroll {
                Box(Modifier.padding(Dimens.space4)) {
                    content(state.data)
                }
            }
    }
}
