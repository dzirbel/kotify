package com.dzirbel.kotify.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.ui.common.VerticalScroll
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.HandleState

@Composable
fun <T> BoxScope.ScrollingPage(
    scrollState: ScrollState,
    presenter: Presenter<T?, *>,
    content: @Composable (T) -> Unit
) {
    HandleState(
        state = { presenter.state() },
        onError = { throwable ->
            Column(Modifier.align(Alignment.Center)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    modifier = Modifier.size(Dimens.iconHuge).align(Alignment.CenterHorizontally),
                    tint = Colors.current.error
                )

                Text(
                    text = "Encountered an error: ${throwable.message}",
                    color = Colors.current.error,
                    fontSize = Dimens.fontTitle
                )

                Text(
                    text = throwable.stackTraceToString(),
                    color = Colors.current.error,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = { presenter.clearError() }
                ) {
                    Text("Clear")
                }
            }
        },
        onLoading = {
            CircularProgressIndicator(Modifier.size(Dimens.iconHuge).align(Alignment.Center))
        },
        onSuccess = {
            VerticalScroll(scrollState = scrollState) {
                Box(Modifier.padding(Dimens.space4)) {
                    content(it)
                }
            }
        }
    )
}
