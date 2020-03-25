package com.dominiczirbel

import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.Track
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

data class ArtistProperties(val id: String, val name: String) {
    fun check(artist: Artist) {
        assertEquals(id, artist.id)
        assertEquals(name, artist.name)
        assertEquals("artist", artist.type)
        assertNotNull(artist.href)
        assertNotNull(artist.uri)
        assertNotNull(artist.externalUrls)
    }
}

data class TrackProperties(
    val id: String,
    val name: String,
    val artistNames: Set<String>,
    val discNumber: Int = 1,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    val trackNumber: Int
) {
    fun check(track: Track, trackRelinking: Boolean = false) {
        assertEquals(id, track.id)
        assertEquals(name, track.name)
        assertEquals("track", track.type)
        assertEquals(artistNames, track.artists.map { it.name }.toSet())
        assertEquals(trackNumber, track.trackNumber)
        assertEquals(discNumber, track.discNumber)
        assertTrue(track.durationMs > 0)
        assertEquals(explicit, track.explicit)
        assertEquals(isLocal, track.isLocal)
        assertNotNull(track.href)
        assertNotNull(track.uri)
        assertNotNull(track.externalUrls)
        if (!trackRelinking) {
            assertNull(track.isPlayable)
            assertNull(track.linkedFrom)
            assertNull(track.restrictions)
        }
    }
}

internal object Fixtures {
    val notFoundId = "a".repeat(22)

    val artists = listOf(
        ArtistProperties(id = "4Lm0pUvmisUHMdoky5ch2I", name = "Apocalyptica"),
        ArtistProperties(id = "4mIdyUBqjS36BQHYFBbGjm", name = "Dalriada")
    )

    val tracks = listOf(
        TrackProperties(
            id = "1rBhSseb3q55BJdTdxuSf4",
            name = "Walk Unafraid",
            artistNames = setOf("R.E.M."),
            trackNumber = 9
        ),

        TrackProperties(
            id = "4d5ft4QeC3GuHBwPGLnLc8",
            name = "Help",
            artistNames = setOf("Thaehan"),
            trackNumber = 13
        ),

        TrackProperties(
            id = "7JyeTAshXQRlFSLJmgr9E6",
            name = "Nausicaa Requiem (Nausicaa Of The Valley Of The Wind)",
            artistNames = setOf("Imaginary Flying Machines", "Neroargento", "Yoko Hallelujah"),
            trackNumber = 12
        )
    )
}
