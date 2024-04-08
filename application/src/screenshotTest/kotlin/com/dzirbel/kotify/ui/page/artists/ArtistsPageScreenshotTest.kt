package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.repository.FakeArtistRepository
import com.dzirbel.kotify.repository.FakeSavedArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.ui.ProvideFakeRepositories
import com.dzirbel.kotify.ui.page.FakeImageViewModel
import com.dzirbel.kotify.ui.page.render
import com.dzirbel.kotify.ui.themedScreenshotTest
import com.dzirbel.kotify.util.MockedTimeExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockedTimeExtension::class)
internal class ArtistsPageScreenshotTest {
    @Test
    fun empty() {
        themedScreenshotTest(filename = "empty") {
            ProvideFakeRepositories {
                ArtistsPage.render()
            }
        }
    }

    @Test
    fun full() {
        val artists = List(10) {
            ArtistViewModel(id = "$it", name = "Artist $it", uri = "artist-$it", images = FakeImageViewModel())
        }

        themedScreenshotTest(filename = "full") {
            ProvideFakeRepositories(
                artistRepository = FakeArtistRepository(artists),
                savedArtistRepository = FakeSavedArtistRepository(artists.associate { it.id to true }),
            ) {
                ArtistsPage.render()
            }
        }
    }
}
