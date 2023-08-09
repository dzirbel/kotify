package com.dzirbel.kotify.ui.page.artists

import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.repository.ArtistList
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.mockArtistTracksNull
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockStates
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.util.MockedTimeExtension
import com.dzirbel.kotify.util.withMockedObjects
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class, MockedTimeExtension::class)
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
        val artists = ArtistList(count = 10).map { ArtistViewModel(it) }
        val ids = artists.map { it.id }

        withMockedObjects(ArtistRepository, SavedArtistRepository, ArtistTracksRepository) {
            SavedArtistRepository.mockLibrary(ids = ids.toSet())
            ArtistRepository.mockStates(ids = ids, values = artists)
            ArtistTracksRepository.mockArtistTracksNull()

            screenshotTest(filename = "full") {
                ArtistsPage.render()
            }
        }
    }
}
