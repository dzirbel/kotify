package com.dzirbel.kotify.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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

/**
 * Standard wrapper around a [presenter] which can display a [RemoteState] with a view model of type [T].
 *
 * Handles the various error, loading, not-found, and success cases of the [presenter]'s current state in standard ways,
 * displaying [content] when a [T] view model is available.
 *
 * May optionally specify a [header] placed at the top of the loaded page which invokes [onHeaderVisibilityChanged] when
 * it is scrolled into or out of the view.
 */
@Composable
fun <T> BoxScope.StandardPage(
    scrollState: ScrollState,
    presenter: Presenter<RemoteState<T>, *>,
    header: @Composable (BoxScope.(T) -> Unit)? = null,
    onHeaderVisibilityChanged: (visible: Boolean) -> Unit = {},
    content: @Composable ColumnScope.(T) -> Unit,
) {
    when (val stateOrError = presenter.state()) {
        is Presenter.StateOrError.Error -> ErrorPage(throwable = stateOrError.throwable, presenter = presenter)
        is Presenter.StateOrError.State -> when (val state = stateOrError.state) {
            is RemoteState.Loaded -> LoadedPage(
                state = state.viewModel,
                scrollState = scrollState,
                content = content,
                header = header,
                onHeaderVisibilityChanged = onHeaderVisibilityChanged,
            )
            is RemoteState.Loading -> LoadingPage()
            is RemoteState.NotFound -> NotFoundPage()
        }
    }
}

@Composable
private fun <T> LoadedPage(
    state: T,
    scrollState: ScrollState,
    content: @Composable ColumnScope.(T) -> Unit,
    header: @Composable (BoxScope.(T) -> Unit)?,
    onHeaderVisibilityChanged: (visible: Boolean) -> Unit,
) {
    Box {
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
            fontSize = Dimens.fontTitle,
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

@Composable
private fun BoxScope.LoadingPage() {
    CircularProgressIndicator(Modifier.size(Dimens.iconHuge).align(Alignment.Center))
}

@Composable
private fun BoxScope.NotFoundPage() {
    // TODO finish 404 page contents
    Text(
        modifier = Modifier.align(Alignment.Center),
        text = "404 Not found",
    )
}
