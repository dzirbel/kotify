package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.framework.PresenterPage
import kotlinx.coroutines.CoroutineScope

object TracksPage : PresenterPage<TracksPresenter.ViewModel, TracksPresenter>() {
    override fun createPresenter(scope: CoroutineScope) = TracksPresenter(scope)

    @Composable
    override fun Header(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
        TracksPageHeader(presenter, state)
    }

    @Composable
    override fun Content(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
        TracksPageContent(presenter, state)
    }

    override fun titleFor(data: TracksPresenter.ViewModel) = "Saved Tracks"
}
