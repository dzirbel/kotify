package com.dzirbel.kotify.ui.page.artist

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.repository.Artist
import com.dzirbel.kotify.repository.ArtistAlbumList
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
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
        val artist = Artist(fullUpdateTime = now, albumsFetched = now)
        val artistAlbums = ArtistAlbumList(artistId = artist.id.value, count = 20)

        KotifyDatabase.blockingTransaction {
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
