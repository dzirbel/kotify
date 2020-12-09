package com.dominiczirbel

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.Playlist
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.network.model.Show
import com.dominiczirbel.network.model.SpotifyObject
import com.dominiczirbel.network.model.Track
import com.google.common.truth.Truth.assertThat

abstract class ObjectProperties(
    private val type: String,
    private val hrefNull: Boolean = false,
    private val uriNull: Boolean = false
) {
    abstract val id: String?
    abstract val name: String

    protected fun check(obj: SpotifyObject) {
        assertThat(obj.id).isEqualTo(id)
        assertThat(obj.name).isEqualTo(name)
        assertThat(obj.type).isEqualTo(type)
        assertThat(obj.href).isNullIf(hrefNull)
        assertThat(obj.uri).isNullIf(uriNull)
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

data class PlaylistProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val tracks: List<TrackProperties>? = null
) : ObjectProperties(type = "playlist") {
    fun check(playlist: Playlist) {
        super.check(playlist)

        assertThat(playlist.description).isEqualTo(description)
        if (tracks != null && playlist is FullPlaylist) {
            tracks.zip(playlist.tracks.items).forEach { (trackProperties, playlistTrack) ->
                trackProperties.check(playlistTrack)
            }
        }
    }
}

data class ShowProperties(
    override val id: String,
    override val name: String,
    val description: String
) : ObjectProperties(type = "show") {
    fun check(show: Show) {
        super.check(show)

        assertThat(show.description).isEqualTo(description)
    }
}

data class TrackProperties(
    override val id: String?,
    override val name: String,
    val artistNames: Set<String>,
    val discNumber: Int = 1,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    val trackNumber: Int
) : ObjectProperties(type = "track", hrefNull = isLocal) {
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

    fun check(playlistTrack: PlaylistTrack) {
        assertThat(playlistTrack.isLocal).isEqualTo(isLocal)
        // TODO test addedAt and addedBy
        check(playlistTrack.track)
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

    val playlists = listOf(
        PlaylistProperties(
            id = "5apAth0JL9APnjo62F93RN",
            name = "Favorites",
            description = ""
        ),
        PlaylistProperties(
            id = "2kLnoVdQ8nNJZ0R09b6fPT",
            name = "Test Playlist",
            description = "test description",
            tracks = listOf(
                TrackProperties(
                    id = "1lTqjO8itiTjspDlZ6EDtv",
                    name = "Grace (feat. Hotei)",
                    artistNames = setOf("Apocalyptica", "HOTEI"),
                    trackNumber = 2
                ),
                TrackProperties(
                    id = null,
                    name = "Beyond Earth",
                    artistNames = setOf("Oratory"),
                    trackNumber = 0,
                    discNumber = 0,
                    isLocal = true
                ),
                TrackProperties(
                    id = "0hrNeGXIsFXCzGv27hDYlz",
                    name = "Chosen Time",
                    artistNames = setOf("Jeff Loomis"),
                    trackNumber = 8
                )
            )
        )
    )

    val shows = listOf(
        ShowProperties(
            id = "1mNsuXfG95Lf76YQeVMuo1",
            name = "StarTalk Radio",
            description = """
                Science, pop culture and comedy collide on StarTalk Radio! Astrophysicist and Hayden Planetarium
                director Neil deGrasse Tyson, his comic co-hosts, guest celebrities and scientists discuss astronomy,
                physics, and everything else about life in the universe. Keep Looking Up! New episodes premiere Friday
                nights at 7pm ET.
                """.trimIndent().replace('\n', ' ').trim()
        ),
        ShowProperties(
            id = "2mTUnDkuKUkhiueKcVWoP0",
            name = "Up First",
            description = """
                NPR's Up First is the news you need to start your day. The three biggest stories of the day, with
                reporting and analysis from NPR News — in 10 minutes. Available weekdays by 6 a.m. ET, with hosts Rachel
                Martin, Noel King, David Greene and Steve Inskeep. Now available on Saturdays by 8 a.m. ET, with hosts
                Lulu Garcia-Navarro and Scott Simon. Subscribe and listen, then support your local NPR station at
                donate.npr.org.
                """.trimIndent().replace('\n', ' ').trim()
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
