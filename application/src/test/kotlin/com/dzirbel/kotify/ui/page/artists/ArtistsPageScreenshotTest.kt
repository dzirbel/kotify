package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.repository.ArtistList
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockStates
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ArtistsPageScreenshotTest {
    @Test
    fun empty() {
        SavedArtistRepository.mockLibrary(ids = null)

        screenshotTest(filename = "empty") {
            ArtistsPage.render()
        }
    }

    @Test
    fun full() {
        val now = Instant.now()
        val artists = ArtistList(count = 10)
        val ids = artists.map { it.id.value }

        SavedArtistRepository.mockLibrary(ids = ids.toSet(), cacheTime = now)
        ArtistRepository.mockStates(ids = ids, values = artists, cacheTime = now)

        RelativeTimeInfo.withMockedTime(now) {
            screenshotTest(filename = "full") {
                ArtistsPage.render()
            }
        }
    }
}
