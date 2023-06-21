package com.dzirbel.kotify.ui.page.artists

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

object ArtistsPage : PresenterPage<ArtistsPresenter.ViewModel, ArtistsPresenter>() {
    override fun createPresenter(scope: CoroutineScope) = ArtistsPresenter(scope)

    @Composable
    override fun Header(presenter: ArtistsPresenter, state: ArtistsPresenter.ViewModel) {
        ArtistsPageHeader(presenter, state)
    }

    @Composable
    override fun Content(presenter: ArtistsPresenter, state: ArtistsPresenter.ViewModel) {
        ArtistsPageContent(presenter, state)
    }

    override fun titleFor(data: ArtistsPresenter.ViewModel) = "Saved Artists"
}
