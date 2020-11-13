package com.dominiczirbel

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.SpotifyObject
import com.dominiczirbel.network.model.Track
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

sealed class ObjectProperties(private val type: String) {
    abstract val id: String
    abstract val name: String

    protected fun check(obj: SpotifyObject) {
        assertEquals(id, obj.id)
        assertEquals(name, obj.name) { "unexpected name for id $id" }
        assertEquals(type, obj.type)
        assertNotNull(obj.href)
        assertNotNull(obj.uri)
    }
}

data class ArtistProperties(
    override val id: String,
    override val name: String,
    val albums: List<AlbumProperties>
) : ObjectProperties(type = "artist") {
    fun check(artist: Artist) {
        super.check(artist)
        assertNotNull(artist.externalUrls)

        if (artist is FullArtist) {
            assertNotNull(artist.followers)
            assertNotNull(artist.genres)
            assertNotNull(artist.images)
            assertTrue(artist.popularity in 0..100) { "popularity: ${artist.popularity}" }
        }
    }
}

data class AlbumProperties(override val id: String, override val name: String) : ObjectProperties(type = "album") {
    fun check(album: Album) {
        super.check(album)

        assertTrue(album.albumType in Album.Type.values()) { "album type: ${album.albumType}" }
        assertTrue(album.artists.isNotEmpty())
        assertNotNull(album.availableMarkets)
        assertNotNull(album.externalUrls)
        assertNotNull(album.images)
        assertNotNull(album.releaseDate)
        assertNotNull(album.releaseDatePrecision)
        assertNull(album.restrictions)

        if (album is FullAlbum) {
            assertNotNull(album.genres)
            assertNotNull(album.label)
            assertTrue(album.popularity in 0..100) { "popularity: ${album.popularity}" }
            assertTrue(album.tracks.items.isNotEmpty())
        }
    }
}

data class TrackProperties(
    override val id: String,
    override val name: String,
    val artistNames: Set<String>,
    val discNumber: Int = 1,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    val trackNumber: Int
) : ObjectProperties(type = "track") {
    fun check(track: Track, trackRelinking: Boolean = false) {
        super.check(track)

        assertEquals(artistNames, track.artists.map { it.name }.toSet())
        assertEquals(trackNumber, track.trackNumber)
        assertEquals(discNumber, track.discNumber)
        assertTrue(track.durationMs > 0) { "durationMs: ${track.durationMs}" }
        assertEquals(explicit, track.explicit)
        assertEquals(isLocal, track.isLocal)
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

    // TODO add more
    val albums = mapOf(
        AlbumProperties("1Z5Aw68hjd9e17izcGbLSQ", "Kikelet") to listOf(
            TrackProperties(
                id = "5p4S4vvrJO1wB5yxdOKcMl",
                name = "Búcsúzó",
                artistNames = setOf("Dalriada"),
                trackNumber = 1
            ),
            TrackProperties(
                id = "49JyVkrdxsciZiFQ1amvUl",
                name = "Kikelet",
                artistNames = setOf("Dalriada"),
                trackNumber = 2
            ),
            TrackProperties(
                id = "5bLICs3ZgqEe3MYFLCstdP",
                name = "Vándor-Fohász",
                artistNames = setOf("Dalriada"),
                trackNumber = 3
            ),
            TrackProperties(
                id = "5ghCuL8wPjC0yESL7e4xQ5",
                name = "Táltosének",
                artistNames = setOf("Dalriada"),
                trackNumber = 4
            ),
            TrackProperties(
                id = "4Aovdxm75RG9bM1daoGojO",
                name = "Néma Harangok",
                artistNames = setOf("Dalriada"),
                trackNumber = 5
            ),
            TrackProperties(
                id = "40y0VEUdIvVXKsNOOnd1To",
                name = "Szentföld",
                artistNames = setOf("Dalriada"),
                trackNumber = 6
            ),
            TrackProperties(
                id = "1ZowHVI8WFNjtfz7fqh7Rr",
                name = "Tűzhozó",
                artistNames = setOf("Dalriada"),
                trackNumber = 7
            ),
            TrackProperties(
                id = "2FDPS2gYmtXmrIUNsR9qkc",
                name = "Tavasz Dala",
                artistNames = setOf("Dalriada"),
                trackNumber = 8
            ),
            TrackProperties(
                id = "7gZJBbzUNQk36dYz7wrOqX",
                name = "Szondi Két Apródja II.rész",
                artistNames = setOf("Dalriada"),
                trackNumber = 9
            )
        )
    )

    val artists = listOf(
        ArtistProperties(
            id = "4Lm0pUvmisUHMdoky5ch2I",
            name = "Apocalyptica",
            albums = listOf(
                AlbumProperties("18Gt3lLDRHDLGoFVMGwHsN", "Plays Metallica by Four Cellos - A Live Performance"),
                AlbumProperties("1FYyFV2BXQq34Juzb9JYYS", "Wagner Reloaded: Live in Leipzig"),
                AlbumProperties("1H3J2vxFHf0WxYUa9iklzu", "Cell-0"),
                AlbumProperties("1rcVwAd9FXK9ONouJVeRSF", "Worlds Collide"),
                AlbumProperties("2qBC6mNZpLTP0dheus0w3n", "Wagner Reloaded: Live in Leipzig"),
                AlbumProperties("34RyPrb9qW7uSCLGpeLrBQ", "Shadowmaker"),
                AlbumProperties("3SAl71qdd63DXgbhk5U9ML", "Shadowmaker - Commentary"),
                AlbumProperties("46OuN3LtlXn4GK9qE7OzAp", "7th Symphony"),
                AlbumProperties("4hdqEMOKD7aG2izjUOmk20", "Amplified - A Decade of Reinventing the Cello"),
                AlbumProperties("4TwdbWtWDxgeLykyt4ExRr", "Plays Metallica by Four Cellos (Remastered)"),
                AlbumProperties("5E9y1NhXyYvDz7VpzWffdM", "Shadowmaker - Track by Track Commentary"),
                AlbumProperties("5fw9PyEt5p0Krd9jmQzKPT", "Shadowmaker"),
                AlbumProperties("5jraCBpvLqFRcO2RuRAeWB", "Shadowmaker"),
                AlbumProperties("5lG9ONdudQ38vodYN0rdqf", "Plays Metallica by Four Cellos (Remastered)"),
                AlbumProperties("5LtbqIjHM33GTDoEDiVX5M", "Shadowmaker"),
                AlbumProperties("5ZkGjaVnwZ2CwlYIw1x0Rk", "Shadowmaker"),
                AlbumProperties("6HN2EqksuJcOZg6pDnlcOl", "Shadowmaker"),
                AlbumProperties("728y0VvMcIuamGmKhDpa6X", "7th Symphony"),
                AlbumProperties("7FkhDs6IRwacn029AM7NQu", "Plays Metallica by Four Cellos - a Live Performance"),
                AlbumProperties("7LZNQn0nVJCEUQXfidfizI", "Plays Metallica by Four Cellos (Remastered)")
            )
        ),
        ArtistProperties(
            id = "4mIdyUBqjS36BQHYFBbGjm",
            name = "Dalriada",
            albums = listOf(
                AlbumProperties("09r0cZ1aQuKoHcS40F0wWh", "Mesék, álmok, regék"),
                AlbumProperties("0GMuyid3D0qKbZwpiMQoK4", "Celtic Music & Songs - Ireland"),
                AlbumProperties("1Z5Aw68hjd9e17izcGbLSQ", "Kikelet"),
                AlbumProperties("20tfQomQDgRlElhDPsrS01", "Nyárutó"),
                AlbumProperties("2HQU4177V84JDOFI8AgwbC", "Alternative Flavors of Caledonia"),
                AlbumProperties("2HrkiIP2NzNH0KOtVs0ILI", "Robert Burns - The Poems and the Music"),
                AlbumProperties("2Yedn1ga2wg3QTVEfvuPDP", "Jégbontó"),
                AlbumProperties("3DIDgySJcb6MsfBMjcnqBG", "Arany-Album"),
                AlbumProperties("3ED6HUUV0MtC1aiHLPOpbU", "Kalapács - Kikalapált dalok"),
                AlbumProperties("3oYnpsZG6kgnJIHf8Y659H", "Jégbontó"),
                AlbumProperties("3Rt5otPyF6OqMfO1wjN46o", "Ezer csillag"),
                AlbumProperties("43EQHlJYWqZonZEG7DQPuR", "Egyek vagyunk (Ossian Tribute)"),
                AlbumProperties("447wr2DtMcrrIIoRlkJr8w", "Ígéret"),
                AlbumProperties("44PjwxnXvAD8HNz1DqwpQf", "Szelek"),
                AlbumProperties("596NzuBPZUZAfO5DZmdDJz", "Napisten Hava"),
                AlbumProperties("5X7vHeH7hoHg1aJIHi3aYY", "Csillagok dala"),
                AlbumProperties("6xcz5gX6lIFuO8eRcWlR0k", "Áldás"),
                AlbumProperties("6yxK3hbXOsxXSeJbKmnRx5", "Fergeteg"),
                AlbumProperties("77MdutiO7oxFTRU1bO6pvB", "Csillagok dala"),
                AlbumProperties("7AmTFZLUdkKHLjSjJpzMW9", "Forrás")
            )
        )
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
