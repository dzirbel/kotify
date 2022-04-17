package com.dzirbel.kotify.ui.page.artist

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.testTransaction
import com.dzirbel.kotify.ui.screenshotTest
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(DatabaseExtension::class)
internal class ArtistPageScreenshotTest {
    @Test
    fun empty() {
        val state = ArtistPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            ArtistPage(artistId = "id").renderState(state)
        }
    }

    @Test
    fun full() {
        val artist = spyk(FixtureModels.artist())
        val artistAlbums = FixtureModels.artistAlbums(artistId = artist.id.value, count = 20)

        // hack: ensure updated time is rendered consistently as now
        every { artist.fullUpdatedTime } answers { Instant.now() }
        every { artist.albumsFetched } answers { Instant.now() }

        testTransaction {
            artistAlbums.forEach {
                it.album.loadToCache()
                it.album.cached.largestImage.loadToCache()
            }
        }

        val baseState = ArtistPresenter.ViewModel()
        val state = baseState.copy(
            artist = artist,
            artistAlbums = baseState.artistAlbums.withElements(artistAlbums),
        )

        screenshotTest(filename = "full", windowWidth = 1500) {
            ArtistPage(artistId = artist.id.value).renderState(state)
        }
    }
}
