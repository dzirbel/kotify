package com.dominiczirbel

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.SpotifyObject
import com.dominiczirbel.network.model.Track
import com.google.common.truth.Truth.assertThat

abstract class ObjectProperties(private val type: String) {
    abstract val id: String
    abstract val name: String

    protected fun check(obj: SpotifyObject) {
        assertThat(obj.id).isEqualTo(id)
        assertThat(obj.name).isEqualTo(name)
        assertThat(obj.type).isEqualTo(type)
        assertThat(obj.href).isNotNull()
        assertThat(obj.uri).isNotNull()
    }
}

data class ArtistProperties(
    override val id: String,
    override val name: String,
    val albums: List<AlbumProperties>
) : ObjectProperties(type = "artist") {
    fun check(artist: Artist) {
        super.check(artist)
        assertThat(artist.externalUrls).isNotNull()

        if (artist is FullArtist) {
            assertThat(artist.followers).isNotNull()
            assertThat(artist.genres).isNotNull()
            assertThat(artist.images).isNotNull()
            assertThat(artist.popularity).isIn(0..100)
        }
    }
}

data class AlbumProperties(override val id: String, override val name: String) : ObjectProperties(type = "album") {
    fun check(album: Album) {
        super.check(album)

        assertThat(album.albumType).isIn(Album.Type.values().toList())
        assertThat(album.artists).isNotEmpty()
        assertThat(album.availableMarkets).isNotNull()
        assertThat(album.externalUrls).isNotNull()
        assertThat(album.images).isNotNull()
        assertThat(album.releaseDate).isNotNull()
        assertThat(album.releaseDatePrecision).isNotNull()
        assertThat(album.restrictions).isNull()

        if (album is FullAlbum) {
            assertThat(album.genres).isNotNull()
            assertThat(album.label).isNotNull()
            assertThat(album.popularity).isIn(0..100)
            assertThat(album.tracks.items).isNotEmpty()
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
    fun check(track: Track) {
        super.check(track)

        assertThat(track.artists.map { it.name }).containsExactlyElementsIn(artistNames)
        assertThat(track.trackNumber).isEqualTo(trackNumber)
        assertThat(track.discNumber).isEqualTo(discNumber)
        assertThat(track.durationMs).isAtLeast(0)
        assertThat(track.explicit).isEqualTo(explicit)
        assertThat(track.isLocal).isEqualTo(isLocal)
        assertThat(track.externalUrls).isNotNull()
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
