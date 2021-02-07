package com.dominiczirbel

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.Episode
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

data class AlbumProperties(
    override val id: String,
    override val name: String,
    val totalTracks: Int? = null,
    val albumType: Album.Type = Album.Type.ALBUM
) : ObjectProperties(type = "album") {
    fun check(album: Album) {
        super.check(album)

        assertThat(album.albumType).isEqualTo(albumType)
        assertThat(album.artists).isNotEmpty()
        assertThat(album.availableMarkets).isNotNull()
        assertThat(album.externalUrls).isNotNull()
        assertThat(album.images).isNotNull()
        assertThat(album.releaseDate).isNotNull()
        assertThat(album.releaseDatePrecision).isNotNull()
        assertThat(album.restrictions).isNull()
        totalTracks?.let { assertThat(album.totalTracks).isEqualTo(it) }

        if (album is FullAlbum) {
            assertThat(album.genres).isNotNull()
            assertThat(album.label).isNotNull()
            assertThat(album.popularity).isIn(0..100)
            assertThat(album.tracks.items).isNotEmpty()
        }
    }
}

data class EpisodeProperties(
    override val id: String,
    override val name: String,
    private val description: String
) : ObjectProperties(type = "episode") {
    fun check(episode: Episode) {
        super.check(episode)

        assertThat(episode.description).isEqualTo(description)
        assertThat(episode.durationMs).isAtLeast(0)
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
    val trackNumber: Int,
    val addedBy: String? = null,
    val addedAt: String? = null
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
        check(playlistTrack.track)

        assertThat(playlistTrack.isLocal).isEqualTo(isLocal)
        addedBy?.let { assertThat(playlistTrack.addedBy.id).isEqualTo(it) }
        addedAt?.let { assertThat(playlistTrack.addedAt).isEqualTo(it) }
    }
}

internal object Fixtures {
    val notFoundId = "a".repeat(22)

    // map from artist ID to whether or not the test users is following the artist
    val followingArtists = listOf(
        "5HA5aLY3jJV7eimXWkRBBp" to true, // Epica
        "6pRi6EIPXz4QJEOEsBaA0m" to false, // Chris Tomlin
        "3hE8S8ohRErocpkY7uJW4a" to true, // Within Temptation
        "3YCKuqpv9nCsIhJ2v8SMix" to false, // Elevation Worship
        "2KaW48xlLnXC2v8tvyhWsa" to true, // Amaranthe
    )

    // map from user ID to whether or not the test user is following the user
    val followingUsers = listOf(
        "luckyeights" to true,
        "matthew.d.zirbel" to true,
        "bobbytonelli" to false,
    )

    // map from playlist ID to a map from user ID to whether the user is following that playlist
    val followingPlaylists = listOf(
        "5apAth0JL9APnjo62F93RN" to mapOf("djynth" to true, "luckyeights" to false),
        "6urDFlFQIDXPwXbfpdGUc0" to mapOf("djynth" to true, "1267916582" to true)
    )

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
        ),

        AlbumProperties("7sDOBekGFHH2KfwW0vn6Me", "Arcane Astral Aeons") to listOf(
            TrackProperties(
                id = "3fsWHLlp3D9CzZv6YUo3pv",
                name = "In Styx Embrace",
                artistNames = setOf("Sirenia"),
                trackNumber = 1
            ),
            TrackProperties(
                id = "6mHngSw9ywDzghL8YvsDnW",
                name = "Into the Night",
                artistNames = setOf("Sirenia"),
                trackNumber = 2
            ),
            TrackProperties(
                id = "52UrdP9ByyzpzdxrKOkQhm",
                name = "Love Like Cyanide",
                artistNames = setOf("Sirenia"),
                trackNumber = 3
            ),
            TrackProperties(
                id = "6ety9hMPEGoOso9jm01ogh",
                name = "Desire",
                artistNames = setOf("Sirenia"),
                trackNumber = 4
            ),
            TrackProperties(
                id = "1Q5zahML4WCp6I2cOHoL4E",
                name = "Asphyxia",
                artistNames = setOf("Sirenia"),
                trackNumber = 5
            ),
            TrackProperties(
                id = "037N6MX5jyhu5YXpyi7id2",
                name = "Queen of Lies",
                artistNames = setOf("Sirenia"),
                trackNumber = 6
            ),
            TrackProperties(
                id = "4of1AAmOGeHQ91pV8hug5S",
                name = "Nos Heures Sombres",
                artistNames = setOf("Sirenia"),
                trackNumber = 7
            ),
            TrackProperties(
                id = "0lzlpdsGRbHqfyMBcGNqSy",
                name = "The Voyage",
                artistNames = setOf("Sirenia"),
                trackNumber = 8
            ),
            TrackProperties(
                id = "1w4ibHOzLSQNl7bYdB5GnF",
                name = "Aerodyne",
                artistNames = setOf("Sirenia"),
                trackNumber = 9
            ),
            TrackProperties(
                id = "1BLrZ3IpeiKsPUjQDWyTsB",
                name = "The Twilight Hour",
                artistNames = setOf("Sirenia"),
                trackNumber = 10
            ),
            TrackProperties(
                id = "1dQTItrEzPEoWj5k3s2Bsy",
                name = "Glowing Embers",
                artistNames = setOf("Sirenia"),
                trackNumber = 11
            ),
            TrackProperties(
                id = "16JomutDWxRTgzeeFTYUdW",
                name = "Love Like Cyanide - Edit",
                artistNames = setOf("Sirenia"),
                trackNumber = 12
            ),
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
            id = "7IxOJnsT8vXhTTzb6nlPOO",
            name = "Trees of Eternity",
            albums = listOf(
                AlbumProperties("6sFhi9TivgwN6XzcEYcfAy", "Hour of the Nightingale", totalTracks = 10)
            )
        ),

        ArtistProperties(
            id = "766wIvoqqGrjRDnExOjJls",
            name = "Thirteen Senses",
            albums = listOf(
                AlbumProperties("5P7GIlX83brISe8k0XQQL1", "A Strange Encounter", totalTracks = 10),
                AlbumProperties("148kHVSDW2cwyAnmOUnSsm", "Crystal Sounds", totalTracks = 10),
                AlbumProperties("1v0kIX9QOYJhmbixRoWpeY", "Contact", totalTracks = 10),
                AlbumProperties("6ua9tnBfjtFbEvlwwPePNE", "Contact", totalTracks = 11),
                AlbumProperties("53d6xPe9mO7wVUCCPqlUqb", "The Invitation", totalTracks = 12),
                AlbumProperties("7JSZOMOVibNSpGWSfcgqcN", "The Invitation", totalTracks = 12),
                AlbumProperties("2taMI79KWzXO0cOV4RJx4i", "The Invitation", totalTracks = 11),
                AlbumProperties(
                    id = "2Ea6nI5gRsy7QXCYFihMEk",
                    name = "Into The Fire (Acoustic)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "5nU3OZkQWBJnz1SijLLepR",
                    name = "Into the Fire (Acoustic)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties("3dXyenZqQJOktXiLRBXU55", "Home", totalTracks = 2, albumType = Album.Type.SINGLE),
                AlbumProperties(
                    id = "3CcltJjnlBarLBrnbU7Dgg",
                    name = "The Loneliest Star",
                    totalTracks = 2,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "4gUjTWrUEcc3Dc2M1k9Jj4",
                    name = "All The Love In Your Hands",
                    totalTracks = 3,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "5bb11iSUgn7dYtodcW5fhW",
                    name = "All The Love In Your Hands (Acoustic Version)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "7i6IV55nXu52k7dqckivO7",
                    name = "All The Love In Your Hands (Cicada Remix Esingle)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "1UYhCN5SOV713pJcCBQ7Cf",
                    name = "All The Love In Your Hands (Qattara Remix Esingle)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties("7ETiCkWai8cmWbVfnKCLJ6", "Follow Me", totalTracks = 3, albumType = Album.Type.SINGLE),
                AlbumProperties(
                    id = "75Rh0bIPEP7IJMUzpGo4os",
                    name = "Into The Fire (Cicada Remix)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "14pHgW7BaEclVGKiw4xhAN",
                    name = "Thru The Glass",
                    totalTracks = 2,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "5Y1ybrFh0aScu26vW7HNwf",
                    name = "Thru The Glass (On-Line Exclusive)",
                    totalTracks = 1,
                    albumType = Album.Type.SINGLE
                ),
                AlbumProperties(
                    id = "6Qh7aqH8eZq6ttLEaLeaOK",
                    name = "Do No Wrong",
                    totalTracks = 2,
                    albumType = Album.Type.SINGLE
                ),
            )
        )
    )

    val episodes = listOf(
        EpisodeProperties(
            id = "4D5tzjNC8WkIASQMn4NrcA",
            name = "Why monkeys (and humans) are wired for fairness | Sarah Brosnan",
            description = """
                         Fairness matters ... to both people and primates. Sharing priceless footage of capuchin monkeys
                responding to perceived injustice, primatologist Sarah Brosnan explores why humans and monkeys evolved
                to care about equality -- and emphasizes the connection between a healthy, cooperative society and
                everyone getting their fair share.       
                """
                .toSingleLine()
        ),
        EpisodeProperties(
            id = "61i9zd2aluBye0NiSf6NOh",
            name = "#1581 - J. Prince",
            description = """
                J. Prince is the CEO of Rap-A-Lot Records, author of The Art & Science of Respect, and founder of The
                Loyalty Collection, a limited collection of fine wines. 
                """
                .toSingleLine()
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
                    trackNumber = 2,
                    addedBy = "djynth",
                    addedAt = "2020-12-06T01:18:37Z"
                ),
                TrackProperties(
                    id = null,
                    name = "Beyond Earth",
                    artistNames = setOf("Oratory"),
                    trackNumber = 0,
                    discNumber = 0,
                    isLocal = true,
                    addedBy = "djynth",
                    addedAt = "2020-12-06T01:24:17Z"
                ),
                TrackProperties(
                    id = "0hrNeGXIsFXCzGv27hDYlz",
                    name = "Chosen Time",
                    artistNames = setOf("Jeff Loomis"),
                    trackNumber = 8,
                    addedBy = "djynth",
                    addedAt = "2020-12-06T01:24:40Z"
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
                """
                .toSingleLine()
        ),
        ShowProperties(
            id = "2mTUnDkuKUkhiueKcVWoP0",
            name = "Up First",
            description = """
                NPR's Up First is the news you need to start your day. The three biggest stories of the day, with
                reporting and analysis from NPR News — in 10 minutes. Available weekdays by 6 a.m. ET, with hosts Rachel
                Martin, Noel King and Steve Inskeep. Now available on Saturdays by 8 a.m. ET, with hosts Lulu
                Garcia-Navarro and Scott Simon. Subscribe and listen, then support your local NPR station at
                donate.npr.org.
                """
                .toSingleLine()
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
        ),

        TrackProperties(
            id = "3Or10XF8LCimAlD8k4TmCn",
            name = "We Believe",
            artistNames = setOf("Red Hot Chili Peppers"),
            trackNumber = 12,
            discNumber = 2
        ),

        TrackProperties(
            id = "3I4qPl71TjjB8cXT2QlRb5",
            name = "Delusion",
            artistNames = setOf("We Are the Catalyst"),
            trackNumber = 1,
            explicit = true
        )
    )
}

/**
 * Converts this multi-line string to a single-line one, with the indents and newlines removed (and replaced with single
 * spaces).
 */
private fun String.toSingleLine(): String {
    return this.trimIndent().trim('\n').replace('\n', ' ')
}
