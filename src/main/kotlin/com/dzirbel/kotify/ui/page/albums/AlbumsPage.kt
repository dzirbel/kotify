package com.dzirbel.kotify.ui.page.albums

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

object AlbumsPage : Page<Unit> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit) {
        BindPresenterPage(
            visible = visible,
            createPresenter = { scope -> AlbumsPresenter(scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> AlbumsPageHeader(presenter, state) },
            content = { presenter, state -> AlbumsPageContent(presenter, state) },
        )
    }

    override fun titleFor(data: Unit) = "Saved Albums"
}
