package com.dzirbel.kotify.ui.page

import androidx.compose.runtime.Composable

/**
 * Simple wrapper to render this [Page] via [Page.bind], for use in screenshot tests.
 */
@Composable
fun Page.render() {
    val pageScope = object : PageScope {
        @Composable
        override fun DisplayPage(
            title: String?,
            content: @Composable (setNavigationTitleVisible: (Boolean) -> Unit) -> Unit,
        ) {
            content {}
        }
    }

    pageScope.bind()
}
