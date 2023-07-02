package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.repository.ArtistList
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
        val artists = ArtistList(count = 10)

        KotifyDatabase.blockingTransaction {
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
