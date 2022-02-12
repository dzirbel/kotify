package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.components.Page

data class PlaylistPage(val playlistId: String) : Page {
    fun titleFor(playlist: Playlist) = "Playlist: ${playlist.name}"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Playlist(this@PlaylistPage)
}
