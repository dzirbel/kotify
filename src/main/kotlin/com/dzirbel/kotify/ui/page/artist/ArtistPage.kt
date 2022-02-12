package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.ui.components.Page

data class ArtistPage(val artistId: String) : Page {
    fun titleFor(artist: Artist) = "Artist: ${artist.name}"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Artist(this@ArtistPage)
}
