package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

data class ArtistPage(val artistId: String) : Page<Artist?> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit): Artist? {
        return BindPresenterPage(
            visible = visible,
            key = artistId,
            createPresenter = { scope -> ArtistPresenter(artistId, scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> ArtistPageHeader(presenter, state) },
            content = { presenter, state -> ArtistPageContent(presenter, state) },
        )
            .artist
    }

    override fun titleFor(data: Artist?) = data?.name
}
