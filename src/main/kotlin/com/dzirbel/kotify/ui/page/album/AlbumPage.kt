package com.dzirbel.kotify.ui.page.album

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

data class AlbumPage(val albumId: String) : Page {
    fun titleFor(album: Album) = "Album: ${album.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Album(pageStack, this@AlbumPage)
    }
}
