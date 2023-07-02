package com.dzirbel.kotify.ui.page.library

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.page.library.albums.AlbumsLibraryState
import com.dzirbel.kotify.ui.page.library.albums.AlbumsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.artists.ArtistsLibraryState
import com.dzirbel.kotify.ui.page.library.artists.ArtistsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.playlists.PlaylistsLibraryState
import com.dzirbel.kotify.ui.page.library.playlists.PlaylistsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.ratings.RatingsLibraryState
import com.dzirbel.kotify.ui.page.library.ratings.RatingsLibraryStatePresenter
import com.dzirbel.kotify.ui.page.library.tracks.TracksLibraryState
import com.dzirbel.kotify.ui.page.library.tracks.TracksLibraryStatePresenter
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun LibraryState(
    scrollState: ScrollState,
    artistsLibraryStatePresenter: ArtistsLibraryStatePresenter,
    albumsLibraryStatePresenter: AlbumsLibraryStatePresenter,
    tracksLibraryStatePresenter: TracksLibraryStatePresenter,
    playlistsLibraryStatePresenter: PlaylistsLibraryStatePresenter,
    ratingsLibraryStatePresenter: RatingsLibraryStatePresenter,
) {
    VerticalScroll(scrollState = scrollState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.space3),
        ) {
            Text("Library State", style = MaterialTheme.typography.h4)

            ArtistsLibraryState(presenter = artistsLibraryStatePresenter)

            HorizontalDivider()

            AlbumsLibraryState(presenter = albumsLibraryStatePresenter)

            HorizontalDivider()

            TracksLibraryState(presenter = tracksLibraryStatePresenter)

            HorizontalDivider()

            PlaylistsLibraryState(presenter = playlistsLibraryStatePresenter)

            HorizontalDivider()

            RatingsLibraryState(presenter = ratingsLibraryStatePresenter)
        }
    }
}
