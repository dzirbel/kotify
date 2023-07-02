package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

data class PlaylistPage(val playlistId: String) :
    PresenterPage<PlaylistPresenter.ViewModel, PlaylistPresenter>(key = playlistId) {
    override fun createPresenter(scope: CoroutineScope) = PlaylistPresenter(playlistId, scope)

    @Composable
    override fun Header(presenter: PlaylistPresenter, state: PlaylistPresenter.ViewModel) {
        PlaylistPageHeader(presenter, state)
    }

    @Composable
    override fun Content(presenter: PlaylistPresenter, state: PlaylistPresenter.ViewModel) {
        PlaylistPageContent(presenter, state)
    }

    override fun titleFor(data: PlaylistPresenter.ViewModel) = data.playlist?.name
}
