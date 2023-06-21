package com.dzirbel.kotify.ui.page.artist

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

data class ArtistPage(val artistId: String) :
    PresenterPage<ArtistPresenter.ViewModel, ArtistPresenter>(key = artistId) {
    override fun createPresenter(scope: CoroutineScope) = ArtistPresenter(artistId, scope)

    @Composable
    override fun Header(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
        ArtistPageHeader(presenter, state)
    }

    @Composable
    override fun Content(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
        ArtistPageContent(presenter, state)
    }

    override fun titleFor(data: ArtistPresenter.ViewModel) = data.artist?.name
}
