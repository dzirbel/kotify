package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

object TracksPage : Page<Unit> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit) {
        BindPresenterPage(
            visible = visible,
            createPresenter = { scope -> TracksPresenter(scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> TracksPageHeader(presenter, state) },
            content = { presenter, state -> TracksPageContent(presenter, state) },
        )
    }

    override fun titleFor(data: Unit) = "Saved Tracks"
}
