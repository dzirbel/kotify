package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

data class AlbumPage(val albumId: String) : Page<Album?> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit): Album? {
        return BindPresenterPage(
            visible = visible,
            key = albumId,
            createPresenter = { scope -> AlbumPresenter(albumId, scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> AlbumPageHeader(presenter, state) },
            content = { presenter, state -> AlbumPageContent(presenter, state) },
        )
            .album
    }

    override fun titleFor(data: Album?) = data?.name
}
