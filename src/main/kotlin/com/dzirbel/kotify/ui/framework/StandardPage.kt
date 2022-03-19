package com.dzirbel.kotify.ui.framework

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

@Composable
fun <T> BoxScope.StandardPage(
    scrollState: ScrollState,
    presenter: Presenter<T, *>,
    header: @Composable (BoxScope.(T) -> Unit)? = null,
    onHeaderVisibilityChanged: (visible: Boolean) -> Unit = {},
    content: @Composable ColumnScope.(T) -> Unit,
) {
    when (val stateOrError = presenter.state()) {
        is Presenter.StateOrError.Error -> ErrorPage(throwable = stateOrError.throwable, presenter = presenter)
        is Presenter.StateOrError.State -> LoadedPage(
            state = stateOrError.state,
            scrollState = scrollState,
            content = content,
            header = header,
            onHeaderVisibilityChanged = onHeaderVisibilityChanged,
        )
    }
}

@Composable
fun ColumnScope.StandardLoadingSpinner() {
    // TODO center vertically; this is difficult since this is typically placed inside a scrolling column
    CircularProgressIndicator(
        Modifier
            .padding(vertical = Dimens.space5)
            .size(Dimens.iconHuge)
            .align(Alignment.CenterHorizontally)
    )
}

@Composable
private fun <T> BoxScope.LoadedPage(
    state: T,
    scrollState: ScrollState,
    content: @Composable ColumnScope.(T) -> Unit,
    header: @Composable (BoxScope.(T) -> Unit)?,
    onHeaderVisibilityChanged: (visible: Boolean) -> Unit,
) {
    Column(Modifier.verticalScroll(scrollState)) {
        if (header != null) {
            val headerVisible = remember { mutableStateOf<Boolean?>(null) }
            Box(
                modifier = Modifier.onGloballyPositioned {
                    val visible = it.boundsInParent().bottom - scrollState.value > 0
                    if (visible != headerVisible.value) {
                        headerVisible.value = visible
                        onHeaderVisibilityChanged(visible)
                    }
                }
            ) {
                header(state)
            }
        }

        content(state)
    }

    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd),
        adapter = rememberScrollbarAdapter(scrollState),
    )
}

@Composable
private fun <T> BoxScope.ErrorPage(throwable: Throwable, presenter: Presenter<T, *>) {
    Column(Modifier.align(Alignment.Center)) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(Dimens.iconHuge).align(Alignment.CenterHorizontally),
            tint = LocalColors.current.error,
        )

        Text(
            text = "Encountered an error: ${throwable.message}",
            color = LocalColors.current.error,
            style = MaterialTheme.typography.h5,
        )

        Text(
            text = throwable.stackTraceToString(),
            color = LocalColors.current.error,
            fontFamily = FontFamily.Monospace,
        )

        Button(
            onClick = { presenter.clearError() }
        ) {
            Text("Clear")
        }
    }
}
