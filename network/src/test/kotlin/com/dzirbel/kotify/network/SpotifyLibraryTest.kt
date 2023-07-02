package com.dzirbel.kotify.network

import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.util.TAG_NETWORK
import com.dzirbel.kotify.util.containsExactlyElementsOf
import com.dzirbel.kotify.util.zipWithBy
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(TAG_NETWORK)
class SpotifyLibraryTest {
    @Test
    fun getSavedAlbums() {
        val albums = runBlocking { Spotify.Library.getSavedAlbums().asFlow().toList() }

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
                },
            )

        val saved = runBlocking { Spotify.Library.checkAlbums(ids.map { it.first }) }

        assertThat(saved).containsExactlyElementsOf(ids.map { it.second })
    }

    @Test
    fun saveAndRemoveAlbums() {
        assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
            .containsExactlyElementsOf(Fixtures.unsavedAlbums.map { false })

        try {
            runBlocking { Spotify.Library.saveAlbums(Fixtures.unsavedAlbums) }

            assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
                .containsExactlyElementsOf(Fixtures.unsavedAlbums.map { true })
        } finally {
            runBlocking { Spotify.Library.removeAlbums(Fixtures.unsavedAlbums) }
        }

        assertThat(runBlocking { Spotify.Library.checkAlbums(Fixtures.unsavedAlbums) })
            .containsExactlyElementsOf(Fixtures.unsavedAlbums.map { false })
    }

    @Test
    fun getSavedTracks() {
        val tracksPaging = runBlocking { Spotify.Library.getSavedTracks() }
        val tracks = runBlocking { tracksPaging.asFlow().toList() }

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
                },
            )

        val saved = runBlocking { Spotify.Library.checkTracks(ids.mapNotNull { it.first }) }

        assertThat(saved).containsExactlyElementsOf(ids.map { it.second })
    }

    @Test
    fun saveAndRemoveTracks() {
        assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
            .containsExactlyElementsOf(Fixtures.unsavedTracks.map { false })

        try {
            runBlocking { Spotify.Library.saveTracks(Fixtures.unsavedTracks) }

            assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
                .containsExactlyElementsOf(Fixtures.unsavedTracks.map { true })
        } finally {
            runBlocking { Spotify.Library.removeTracks(Fixtures.unsavedTracks) }
        }

        assertThat(runBlocking { Spotify.Library.checkTracks(Fixtures.unsavedTracks) })
            .containsExactlyElementsOf(Fixtures.unsavedTracks.map { false })
    }

    @Test
    fun getSavedShows() {
        val showsPaging = runBlocking { Spotify.Library.getSavedShows() }
        val shows = runBlocking { showsPaging.asFlow().toList() }

        val expected = Fixtures.shows.filter { it.saved }

        shows.zipWithBy(expected) { savedShow, showProperties -> savedShow.show.id == showProperties.id }
            .forEach { (savedShow, showProperties) -> showProperties.check(savedShow) }
    }

    @Test
    fun checkShows() {
        val saved = runBlocking { Spotify.Library.checkShows(Fixtures.shows.map { it.id }) }
        assertThat(saved).containsExactlyElementsOf(Fixtures.shows.map { it.saved })
    }

    @Test
    fun saveAndRemoveShows() {
        val unsaved = listOf(Fixtures.shows.first { !it.saved }).map { it.id }

        assertThat(runBlocking { Spotify.Library.checkShows(unsaved) }).all {
            hasSameSizeAs(unsaved)
            each { it.isFalse() }
        }

        try {
            runBlocking { Spotify.Library.saveShows(unsaved) }

            assertThat(runBlocking { Spotify.Library.checkShows(unsaved) }).all {
                hasSameSizeAs(unsaved)
                each { it.isTrue() }
            }
        } finally {
            runBlocking { Spotify.Library.removeShows(unsaved) }
        }

        assertThat(runBlocking { Spotify.Library.checkShows(unsaved) }).all {
            hasSameSizeAs(unsaved)
            each { it.isFalse() }
        }
    }
}
