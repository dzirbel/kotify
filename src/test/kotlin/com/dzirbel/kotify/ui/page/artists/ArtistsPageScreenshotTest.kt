package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.screenshotTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
internal class ArtistsPageScreenshotTest {
    @Test
    fun empty() {
        val state = ArtistsPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            ArtistsPage.renderState(state)
        }
    }

    @Test
    fun full() {
        val artist = runBlocking {
            KotifyDatabase.transaction(name = null) {
                requireNotNull(Artist.from(Fixtures.artist))
                    .also { it.largestImage.loadToCache() }
            }
        }

        val state = ArtistsPresenter.ViewModel(
            artists = ListAdapter.of(listOf(artist)),
        )

        screenshotTest(filename = "full") {
            ArtistsPage.renderState(state)
        }
    }

    private object Fixtures {
        // TODO consolidate in shared fixtures
        val artist = FullSpotifyArtist(
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            id = "artist-1",
            name = "Artist 1",
            type = "artist",
            uri = "uri",
            followers = SpotifyFollowers(total = 42),
            genres = listOf("genre"),
            images = listOf(),
            popularity = 42,
        )
    }
}
