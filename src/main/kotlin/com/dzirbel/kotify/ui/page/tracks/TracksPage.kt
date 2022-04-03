package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

object TracksPage : PresenterPage<TracksPresenter.ViewModel, TracksPresenter>() {
    override fun createPresenter(scope: CoroutineScope) = TracksPresenter(scope)

    @Composable
    override fun header(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
        TracksPageHeader(presenter, state)
    }

    @Composable
    override fun content(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
        TracksPageContent(state)
    }

    override fun titleFor(data: TracksPresenter.ViewModel) = "Saved Tracks"
}
