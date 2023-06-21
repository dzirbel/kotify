package com.dzirbel.kotify.ui.page.artist

import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.testTransaction
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.util.RelativeTimeInfo
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ArtistPageScreenshotTest {
    @Test
    fun empty() {
        val state = ArtistPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            ArtistPage(artistId = "id").RenderState(state)
        }
    }

    @Test
    fun full() {
        val now = Instant.now()
        val artist = FixtureModels.artist(fullUpdateTime = now, albumsFetched = now)
        val artistAlbums = FixtureModels.artistAlbums(artistId = artist.id.value, count = 20)

        testTransaction {
            artistAlbums.forEach { artistAlbum ->
                artistAlbum.album.loadToCache()
                artistAlbum.album.cached.largestImage.loadToCache()
            }
        }

        val baseState = ArtistPresenter.ViewModel()
        val state = baseState.copy(
            artist = artist,
            artistAlbums = baseState.artistAlbums.withElements(artistAlbums),
        )

        RelativeTimeInfo.withMockedTime(now) {
            screenshotTest(filename = "full", windowWidth = 1500) {
                ArtistPage(artistId = artist.id.value).RenderState(state)
            }
        }
    }
}
