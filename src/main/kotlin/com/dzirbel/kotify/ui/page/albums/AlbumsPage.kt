package com.dzirbel.kotify.ui.page.albums

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

object AlbumsPage : PresenterPage<AlbumsPresenter.ViewModel, AlbumsPresenter>() {
    override fun createPresenter(scope: CoroutineScope) = AlbumsPresenter(scope)

    @Composable
    override fun header(presenter: AlbumsPresenter, state: AlbumsPresenter.ViewModel) {
        AlbumsPageHeader(presenter, state)
    }

    @Composable
    override fun content(presenter: AlbumsPresenter, state: AlbumsPresenter.ViewModel) {
        AlbumsPageContent(presenter, state)
    }

    override fun titleFor(data: AlbumsPresenter.ViewModel) = "Saved Albums"
}
