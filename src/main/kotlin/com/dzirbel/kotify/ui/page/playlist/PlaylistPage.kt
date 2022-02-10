package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

data class PlaylistPage(val playlistId: String) : Page {
    fun titleFor(playlist: Playlist) = "Playlist: ${playlist.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Playlist(pageStack, this@PlaylistPage)
    }
}
