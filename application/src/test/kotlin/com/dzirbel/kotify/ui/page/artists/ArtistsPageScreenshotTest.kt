package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.repository.ArtistList
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockStates
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.util.withMockedObjects
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
internal class ArtistsPageScreenshotTest {
    @Test
    fun empty() {
        withMockedObjects(SavedArtistRepository) {
            SavedArtistRepository.mockLibrary(ids = null)

            screenshotTest(filename = "empty") {
                ArtistsPage.render()
            }
        }
    }

    @Test
    fun full() {
        val artists = ArtistList(count = 10)
        val ids = artists.map { it.id.value }

        RelativeTimeInfo.withMockedTime { now ->
            withMockedObjects(ArtistRepository, SavedArtistRepository) {
                SavedArtistRepository.mockLibrary(ids = ids.toSet(), cacheTime = now)
                ArtistRepository.mockStates(ids = ids, values = artists, cacheTime = now)

                screenshotTest(filename = "full") {
                    ArtistsPage.render()
                }
            }
        }
    }
}
