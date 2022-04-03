package com.dzirbel.kotify.ui.page.library

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.page.library.albums.AlbumsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.artists.ArtistsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.playlists.PlaylistsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.ratings.RatingsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.tracks.TracksLibraryStatePresenter

object LibraryStatePage : Page<Unit> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit) {
        val scrollState = rememberScrollState()

        // bind child presenters to attach them to the composition
        val artistsLibraryStatePresenter = rememberPresenter { scope -> ArtistsLibraryStatePresenter(scope) }
        val albumsLibraryStatePresenter = rememberPresenter { scope -> AlbumsLibraryStatePresenter(scope) }
        val tracksLibraryStatePresenter = rememberPresenter { scope -> TracksLibraryStatePresenter(scope) }
        val playlistsLibraryStatePresenter = rememberPresenter { scope -> PlaylistsLibraryStatePresenter(scope) }
        val ratingsLibraryStatePresenter = rememberPresenter { scope -> RatingsLibraryStatePresenter(scope) }

        if (visible) {
            LibraryState(
                scrollState = scrollState,
                artistsLibraryStatePresenter = artistsLibraryStatePresenter,
                albumsLibraryStatePresenter = albumsLibraryStatePresenter,
                tracksLibraryStatePresenter = tracksLibraryStatePresenter,
                playlistsLibraryStatePresenter = playlistsLibraryStatePresenter,
                ratingsLibraryStatePresenter = ratingsLibraryStatePresenter,
            )
        }
    }

    override fun titleFor(data: Unit) = "Library State"
}
