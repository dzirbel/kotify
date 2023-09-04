package com.dzirbel.kotify.ui.framework

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.page.Page
import com.dzirbel.kotify.ui.page.PageScope

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
