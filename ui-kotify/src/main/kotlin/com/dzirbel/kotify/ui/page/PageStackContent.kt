package com.dzirbel.kotify.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders the given [pageStack].
 *
 * @param pageStack the [PageStack] to render
 * @param setTitle callback invoked when the title of the page with the given index should be updated to the given title
 * @param setNavigationTitleVisible callback invoked when the visibility of the title in the navigation header should be
 *  updated
 * @param modifier
 */
@Composable
fun PageStackContent(
    pageStack: PageStack,
    setTitle: (index: Int, title: String?) -> Unit,
    setNavigationTitleVisible: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        for ((index, page) in pageStack.pages.withIndex()) {
            val isCurrent = index == pageStack.currentIndex
            val pageScope = remember(index, isCurrent) {
                PageScopeImpl(
                    visible = isCurrent,
                    setTitle = { setTitle(index, it) },
                    setNavigationTitleVisible = if (isCurrent) {
                        setNavigationTitleVisible
                    } else {
                        {}
                    },
                )
            }
            with(page) { pageScope.bind() }
        }
    }
}

private class PageScopeImpl(
    private val visible: Boolean,
    private val setTitle: (String?) -> Unit,
    private val setNavigationTitleVisible: (Boolean) -> Unit,
) : PageScope {
    @Composable
    override fun DisplayPage(
        title: String?,
        content: @Composable (setNavigationTitleVisible: (Boolean) -> Unit) -> Unit,
    ) {
        setTitle(title)

        if (visible) {
            content(setNavigationTitleVisible)
        }
    }
}
