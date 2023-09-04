package com.dzirbel.kotify.ui.page

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Represents a page in a [PageStack] and how it is rendered.
 */
@Stable
interface Page {
    /**
     * Binds this [Page] to the composition.
     *
     * This [Composable] should not directly render any of the page content, but only attach its state to the
     * composition (e.g. remembering a scroll state or sort order) and call [PageScope.DisplayPage] with the page's
     * composable content. The [PageScope] will then render the page if it is the current page in the stack, otherwise
     * just preserving the state.
     */
    @Composable
    fun PageScope.bind()
}

/**
 * A scope in which a [Page] may be rendered; providing a [DisplayPage] method which renders the page as appropriate.
 */
@Stable
interface PageScope {
    /**
     * Displays the given [Page] content, if it is visible.
     *
     * @param title the title of the page, optionally null while it is being loaded
     * @param content the content of the page, which will be rendered if the page is visible, accepting a callback
     *  allowing the page to toggle the visibility of the [title] in the navigation header
     */
    @Composable
    fun DisplayPage(title: String?, content: @Composable (setNavigationTitleVisibility: (Boolean) -> Unit) -> Unit)

    /**
     * A convenience wrapper on [DisplayPage] which wraps the [content] and [header] in a [VerticalScrollPage], and uses
     * it to determine the state of the [title] in the navigation header.
     */
    @Composable
    fun DisplayVerticalScrollPage(title: String?, header: (@Composable () -> Unit)?, content: @Composable () -> Unit) {
        val scrollState = rememberScrollState()

        DisplayPage(title = title) { setNavigationTitleVisibility ->
            VerticalScrollPage(
                scrollState = scrollState,
                onHeaderVisibilityChanged = { pageHeaderVisible ->
                    setNavigationTitleVisibility(!pageHeaderVisible)
                },
                header = header,
                content = content,
            )
        }
    }
}
