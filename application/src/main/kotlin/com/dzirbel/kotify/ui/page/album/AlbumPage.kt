package com.dzirbel.kotify.ui.page.album

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

data class AlbumPage(val albumId: String) : PresenterPage<AlbumPresenter.ViewModel, AlbumPresenter>(key = albumId) {
    override fun createPresenter(scope: CoroutineScope) = AlbumPresenter(albumId, scope)

    @Composable
    override fun Header(presenter: AlbumPresenter, state: AlbumPresenter.ViewModel) {
        AlbumPageHeader(presenter, state)
    }

    @Composable
    override fun Content(presenter: AlbumPresenter, state: AlbumPresenter.ViewModel) {
        AlbumPageContent(presenter, state)
    }

    override fun titleFor(data: AlbumPresenter.ViewModel) = data.album?.name
}
