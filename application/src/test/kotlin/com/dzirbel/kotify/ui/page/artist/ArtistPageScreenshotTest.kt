package com.dzirbel.kotify.ui.page.artist

import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.repository.Artist
import com.dzirbel.kotify.repository.ArtistAlbumList
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockStateCached
import com.dzirbel.kotify.repository.mockStateNull
import com.dzirbel.kotify.repository.mockStates
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.util.MockedTimeExtension
import com.dzirbel.kotify.util.withMockedObjects
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class, MockedTimeExtension::class)
internal class ArtistPageScreenshotTest {
    @Test
    fun empty() {
        val artistId = "artistId"

        withMockedObjects(ArtistAlbumsRepository, ArtistRepository) {
            ArtistRepository.mockStateNull(id = artistId)
            ArtistAlbumsRepository.mockStateNull(id = artistId)

            screenshotTest(filename = "empty") {
                ArtistPage(artistId = artistId).render()
            }
        }
    }

    @Test
    fun full() {
        val artist = ArtistViewModel(Artist())
        val artistAlbums = ArtistAlbumList(artistId = artist.id, count = 20)
        val artistAlbumViewModels = KotifyDatabase.blockingTransaction {
            artistAlbums.map { ArtistAlbumViewModel(it) }
        }

        withMockedObjects(AlbumTracksRepository, ArtistAlbumsRepository, ArtistRepository, SavedAlbumRepository) {
            ArtistRepository.mockStateCached(id = artist.id, value = artist)
            ArtistAlbumsRepository.mockStateCached(id = artist.id, value = artistAlbumViewModels)
            SavedAlbumRepository.mockLibrary(ids = null)
            AlbumTracksRepository.mockStates(
                ids = artistAlbums.map { it.albumId.value },
                values = artistAlbums.map { emptyList() },
            )

            for (artistAlbum in artistAlbumViewModels) {
                AlbumTracksRepository.mockStateNull(artistAlbum.album.id)
            }

            screenshotTest(filename = "full", windowWidth = 1500) {
                ArtistPage(artistId = artist.id).render()
            }
        }
    }
}
