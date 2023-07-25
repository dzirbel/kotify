package com.dzirbel.kotify.ui.page.artist

import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.repository.Artist
import com.dzirbel.kotify.repository.ArtistAlbumList
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockStateCached
import com.dzirbel.kotify.repository.mockStateNull
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.util.withMockedObjects
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
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
        RelativeTimeInfo.withMockedTime { now ->
            val artist = Artist(fullUpdateTime = now, albumsFetched = now)
            val artistAlbums = ArtistAlbumList(artistId = artist.id.value, count = 20)

            KotifyDatabase.blockingTransaction {
                for (artistAlbum in artistAlbums) {
                    artistAlbum.album.loadToCache()
                }
            }

            withMockedObjects(AlbumTracksRepository, ArtistAlbumsRepository, ArtistRepository, SavedAlbumRepository) {
                ArtistRepository.mockStateCached(id = artist.id.value, value = artist, cacheTime = now)
                ArtistAlbumsRepository.mockStateCached(id = artist.id.value, value = artistAlbums, cacheTime = now)
                SavedAlbumRepository.mockLibrary(ids = null)

                for (album in artistAlbums) {
                    AlbumTracksRepository.mockStateNull(album.albumId.value)
                }

                screenshotTest(filename = "full", windowWidth = 1500) {
                    ArtistPage(artistId = artist.id.value).render()
                }
            }
        }
    }
}
