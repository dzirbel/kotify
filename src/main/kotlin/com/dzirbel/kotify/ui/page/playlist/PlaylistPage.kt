package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.framework.BindPresenterPage

data class PlaylistPage(val playlistId: String) : Page<Playlist?> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit): Playlist? {
        return BindPresenterPage(
            visible = visible,
            key = playlistId,
            createPresenter = { scope -> PlaylistPresenter(playlistId, scope) },
            toggleNavigationTitle = toggleNavigationTitle,
            header = { presenter, state -> PlaylistPageHeader(presenter, state) },
            content = { presenter, state -> PlaylistPageContent(presenter, state) },
        )
            .playlist
    }

    override fun titleFor(data: Playlist?) = data?.name
}
