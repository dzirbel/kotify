package com.dzirbel.kotify.network

import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.TAG_NETWORK
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.network.model.SpotifySavedShow
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.zipWithBy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(TAG_NETWORK)
class SpotifyLibraryTest {
    @Test
    fun getSavedAlbums() {
        val albums = runBlocking { Spotify.Library.getSavedAlbums().fetchAll<SpotifySavedAlbum>() }

        albums.zipWithBy(Fixtures.savedAlbums) { savedAlbum, albumProperties ->
            savedAlbum.album.id == albumProperties.id
        }.forEach { (savedAlbum, albumProperties) -> albumProperties.check(savedAlbum) }
    }

    @Test
    fun checkAlbums() {
        // map from album ID to whether it is saved
        val ids: List<Pair<String, Boolean>> = Fixtures.savedAlbums.map { it.id to true }
            .plus(
                Fixtures.albums.keys.map {
                    it.id to Fixtures.savedAlbums.any { savedAlbum -> it.id == savedAlbum.id }
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
        val tracks = runBlocking { tracksPaging.fetchAll<SpotifySavedTrack>() }

        tracks.zipWithBy(Fixtures.savedTracks) { savedTrack, trackProperties ->
            savedTrack.track.id == trackProperties.id
        }.forEach { (savedTrack, trackProperties) -> trackProperties.check(savedTrack) }
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
        val shows = runBlocking { showsPaging.fetchAll<SpotifySavedShow>() }

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
}
