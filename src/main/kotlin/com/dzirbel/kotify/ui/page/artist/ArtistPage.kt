package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

data class ArtistPage(val artistId: String) : Page {
    fun titleFor(artist: Artist) = "Artist: ${artist.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Artist(pageStack, this@ArtistPage)
    }
}
