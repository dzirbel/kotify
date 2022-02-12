package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.Page

data class AlbumPage(val albumId: String) : Page {
    fun titleFor(album: Album) = "Album: ${album.name}"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Album(this@AlbumPage)
}
