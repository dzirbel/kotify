package com.dzirbel.kotify.ui.framework

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.coroutines.CoroutineScope

/**
 * Abstracts the content of a standard [Page] using standard components for a page rendered by a presenter of type [P].
 *
 * This default [Page] implementation remembers the presenter created by [createPresenter], its state of type [VM], and
 * a [ScrollState] to be remembered in the scope of the composition of [bind] and renders a standard scrolling layout
 * or error page when the page is visible.
 *
 * An optional [key] may be provided which differentiates presenters of the same type in the composition. For example,
 * in order to maintain distinct presenters when pages for two different artists are present on the page stack, the
 * [key] could be the artist ID.
 */
@Stable
abstract class PresenterPage<VM, P : Presenter<VM, *>>(private val key: Any? = null) : Page<VM>() {
    /**
     * Creates an instance of the presenter [P] under the given scope, typically by simply calling its constructor.
     */
    abstract fun createPresenter(scope: CoroutineScope): P

    /**
     * Renders the header of the page with the given [presenter] (in order to emit events) and [state].
     *
     * The page header is rendered above the [Content] and distinct in that when the user scrolls past the header the
     * page title (provided by [titleFor]) is displayed in the navigation bar.
     */
    @Composable
    abstract fun Header(presenter: P, state: VM)

    /**
     * Renders the main content of the page with the given [presenter] (in order to emit events) and [state].
     */
    @Composable
    abstract fun Content(presenter: P, state: VM)

    @Composable
    final override fun BoxScope.bind(visible: Boolean): VM {
        val presenter = rememberPresenter(key = key, createPresenter = ::createPresenter)
        val scrollState = rememberScrollState()
        val stateOrError = presenter.state()

        if (visible) {
            when (stateOrError) {
                is Presenter.StateOrError.Error -> ErrorState(throwable = stateOrError.throwable, presenter = presenter)
                is Presenter.StateOrError.State -> RenderState(
                    presenter = presenter,
                    state = stateOrError.state,
                    scrollState = scrollState,
                    onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
                )
            }
        }

        return stateOrError.safeState
    }

    /**
     * Displays the given successful [state].
     *
     * This is exposed primarily as a convenience function for screenshot tests, which can use it as a simple way to
     * capture the content of a page with a given [state]. As such, the other arguments have defaults which generally
     * should not be used outside of tests.
     */
    @Composable
    fun RenderState(
        state: VM,
        presenter: P = rememberPresenter(key = key, createPresenter = ::createPresenter),
        scrollState: ScrollState = rememberScrollState(),
        onHeaderVisibilityChanged: (visible: Boolean) -> Unit = {},
    ) {
        VerticalScrollPage(
            scrollState = scrollState,
            onHeaderVisibilityChanged = onHeaderVisibilityChanged,
            header = { Header(presenter, state) },
            content = { Content(presenter, state) },
        )
    }

    @Composable
    private fun <T> ErrorState(throwable: Throwable, presenter: Presenter<T, *>) {
        if (throwable is Presenter.NotFound) {
            Box(Modifier.instrument().fillMaxSize().padding(Dimens.space5)) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.h6,
                    text = throwable.message.orEmpty(),
                )
            }
        } else {
            VerticalScroll(columnModifier = Modifier.instrument().padding(Dimens.space5)) {
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

                Button(onClick = { presenter.clearError() }) {
                    Text("Clear")
                }
            }
        }
    }
}
