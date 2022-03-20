package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

object ArtistsPage : Page<Unit> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit) {
        BindPresenterPage(
            visible = visible,
            createPresenter = { scope -> ArtistsPresenter(scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> ArtistsPageHeader(presenter, state) },
            content = { presenter, state -> ArtistsPageContent(presenter, state) },
        )
    }

    override fun titleFor(data: Unit) = "Saved Artists"
}
