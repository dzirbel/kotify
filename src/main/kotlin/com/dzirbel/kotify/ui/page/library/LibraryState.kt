package com.dzirbel.kotify.ui.page.library

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
import com.dzirbel.kotify.ui.page.library.artists.ArtistsLibraryState
import com.dzirbel.kotify.ui.page.library.playlists.PlaylistsLibraryState
import com.dzirbel.kotify.ui.page.library.ratings.RatingsLibraryState
import com.dzirbel.kotify.ui.page.library.tracks.TracksLibraryState
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun LibraryState() {
    VerticalScroll(scrollState = pageStack.value.currentScrollState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.space3),
        ) {
            Text("Library State", style = MaterialTheme.typography.h4)

            ArtistsLibraryState()

            HorizontalDivider()

            AlbumsLibraryState()

            HorizontalDivider()

            TracksLibraryState()

            HorizontalDivider()

            PlaylistsLibraryState()

            HorizontalDivider()

            RatingsLibraryState()
        }
    }
}
