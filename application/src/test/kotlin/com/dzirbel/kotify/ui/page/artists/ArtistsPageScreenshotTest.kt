package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.testTransaction
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.screenshotTest
import org.junit.jupiter.api.Test

internal class ArtistsPageScreenshotTest {
    @Test
    fun empty() {
        val state = ArtistsPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            ArtistsPage.RenderState(state)
        }
    }

    @Test
    fun full() {
        val artists = FixtureModels.artists(count = 10)

        testTransaction {
            artists.forEach { it.largestImage.loadToCache() }
        }

        val state = ArtistsPresenter.ViewModel(
            artists = ListAdapter.of(artists),
        )

        screenshotTest(filename = "full") {
            ArtistsPage.RenderState(state)
        }
    }
}
