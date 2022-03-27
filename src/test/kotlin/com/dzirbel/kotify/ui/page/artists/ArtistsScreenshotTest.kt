package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.screenshotTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

// TODO test header and content together (possibly refactoring BindPresenterPage)
@ExtendWith(DatabaseExtension::class)
internal class ArtistsScreenshotTest {
    @Test
    fun headerEmpty() {
        screenshotTest(filename = "header-empty") {
            val presenter = rememberPresenter { scope -> ArtistsPresenter(scope) }

            ArtistsPageHeader(
                presenter = presenter,
                state = ArtistsPresenter.ViewModel(),
            )
        }
    }

    @Test
    fun headerFull() {
        val artist = runBlocking {
            KotifyDatabase.transaction { requireNotNull(Artist.from(Fixtures.artist)) }
        }

        screenshotTest(filename = "header-full") {
            val presenter = rememberPresenter { scope -> ArtistsPresenter(scope) }

            ArtistsPageHeader(
                presenter = presenter,
                state = ArtistsPresenter.ViewModel(
                    artists = ListAdapter.of(listOf(artist)),
                ),
            )
        }
    }

    @Test
    fun contentEmpty() {
        screenshotTest(filename = "content-empty") {
            val presenter = rememberPresenter { scope -> ArtistsPresenter(scope) }

            ArtistsPageContent(
                presenter = presenter,
                state = ArtistsPresenter.ViewModel(),
            )
        }
    }

    @Test
    fun contentFull() {
        val artist = runBlocking {
            KotifyDatabase.transaction {
                requireNotNull(Artist.from(Fixtures.artist))
                    .also { it.largestImage.loadToCache() }
            }
        }

        screenshotTest(filename = "content-full") {
            val presenter = rememberPresenter { scope -> ArtistsPresenter(scope) }

            ArtistsPageContent(
                presenter = presenter,
                state = ArtistsPresenter.ViewModel(
                    artists = ListAdapter.of(listOf(artist)),
                ),
            )
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
