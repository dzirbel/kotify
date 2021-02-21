package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.network.model.SavedAlbum
import com.dominiczirbel.network.model.SavedShow
import com.dominiczirbel.network.model.SavedTrack
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SpotifyLibraryTest {
    // TODO do as for tracks
    @Test
    fun getSavedAlbums() {
        val albumsPaging = runBlocking { Spotify.Library.getSavedAlbums() }
        val albums = runBlocking { albumsPaging.fetchAll<SavedAlbum>() }

        assertThat(albums).hasSize(Fixtures.savedAlbums.size)
        albums.forEach { (addedAt, album) ->
            val albumPropertiesMap = Fixtures.savedAlbums.filterValues { it.id == album.id }
            assertThat(albumPropertiesMap).hasSize(1)

            val (expectedAddedAt, albumProperties) = albumPropertiesMap.entries.first()
            assertThat(addedAt).isEqualTo(expectedAddedAt)
            albumProperties.check(album)
        }
    }

    @Test
    fun checkAlbums() {
        // map from album ID to whether it is saved
        val ids: List<Pair<String, Boolean>> = Fixtures.savedAlbums.values.map { it.id to true }
            .plus(
                Fixtures.albums.keys.map {
                    it.id to Fixtures.savedAlbums.values.any { savedAlbum -> it.id == savedAlbum.id }
                }
            )

        val saved = runBlocking { Spotify.Library.checkAlbums(ids.map { it.first }) }
        assertThat(saved).containsExactlyElementsIn(ids.map { it.second }).inOrder()
    }

    @Test
    fun saveAndRemoveAlbums() {
        assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
            .containsExactlyElementsIn(Fixtures.unsavedAlbums.map { false })

        runBlocking { Spotify.Library.saveAlbums(Fixtures.unsavedAlbums) }

        assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
            .containsExactlyElementsIn(Fixtures.unsavedAlbums.map { true })

        runBlocking { Spotify.Library.removeAlbums(Fixtures.unsavedAlbums) }

        assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
            .containsExactlyElementsIn(Fixtures.unsavedAlbums.map { false })
    }

    @Test
    fun getSavedTracks() {
        val tracksPaging = runBlocking { Spotify.Library.getSavedTracks() }
        val tracks = runBlocking { tracksPaging.fetchAll<SavedTrack>() }

        assertThat(tracks).hasSize(Fixtures.savedTracks.size)
        tracks.forEach { track ->
            val trackProperties = requireNotNull(Fixtures.savedTracks.find { it.id == track.track.id }) {
                "could not find track $track"
            }
            trackProperties.check(track)
        }
    }

    @Test
    fun checkTracks() {
        // map from track ID to whether it is saved
        val ids: List<Pair<String?, Boolean>> = Fixtures.savedTracks.map { it.id to true }
            .plus(
                Fixtures.tracks.map {
                    it.id to Fixtures.savedTracks.any { savedTrack -> it.id == savedTrack.id }
                }
            )

        val saved = runBlocking { Spotify.Library.checkTracks(ids.mapNotNull { it.first }) }
        assertThat(saved).containsExactlyElementsIn(ids.map { it.second }).inOrder()
    }

    @Test
    fun saveAndRemoveTracks() {
        assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
            .containsExactlyElementsIn(Fixtures.unsavedTracks.map { false })

        runBlocking { Spotify.Library.saveTracks(Fixtures.unsavedTracks) }

        assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
            .containsExactlyElementsIn(Fixtures.unsavedTracks.map { true })

        runBlocking { Spotify.Library.removeTracks(Fixtures.unsavedTracks) }

        assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
            .containsExactlyElementsIn(Fixtures.unsavedTracks.map { false })
    }

    @Test
    fun getSavedShows() {
        val showsPaging = runBlocking { Spotify.Library.getSavedShows() }
        val shows = runBlocking { showsPaging.fetchAll<SavedShow>() }

        val expected = Fixtures.shows.filter { it.saved }

        shows.zipWithBy(expected) { savedShow, showProperties -> savedShow.show.id == showProperties.id }
            .forEach { (savedShow, showProperties) -> showProperties.check(savedShow) }
    }

    @Test
    fun checkShows() {
        val saved = runBlocking { Spotify.Library.checkShows(Fixtures.shows.map { it.id }) }
        assertThat(saved).containsExactlyElementsIn(Fixtures.shows.map { it.saved }).inOrder()
    }

    @Test
    fun saveAndRemoveShows() {
        val unsaved = listOf(Fixtures.shows.first { !it.saved }).map { it.id }

        assertThat(runBlocking { Spotify.Library.checkShows(unsaved) })
            .containsExactlyElementsIn(unsaved.map { false })

        runBlocking { Spotify.Library.saveShows(unsaved) }

        assertThat(runBlocking { Spotify.Library.checkShows(unsaved) })
            .containsExactlyElementsIn(unsaved.map { true })

        runBlocking { Spotify.Library.removeShows(unsaved) }

        assertThat(runBlocking { Spotify.Library.checkShows(unsaved) })
            .containsExactlyElementsIn(unsaved.map { false })
    }

    // TODO use elsewhere
    private fun <T, R> List<T>.zipWithBy(other: List<R>, matcher: (T, R) -> Boolean): List<Pair<T, R>> {
        assertThat(this).hasSize(other.size)
        return map { thisElement ->
            val matching = other.filter { matcher(thisElement, it) }
            assertThat(matching).hasSize(1)

            thisElement to matching.first()
        }
    }
}
