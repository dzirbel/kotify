package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.properties.AlbumProperties
import com.dzirbel.kotify.network.properties.ArtistProperties
import com.dzirbel.kotify.network.properties.EpisodeProperties
import com.dzirbel.kotify.network.properties.PlaylistProperties
import com.dzirbel.kotify.network.properties.ShowProperties
import com.dzirbel.kotify.network.properties.TrackProperties

@Suppress("LargeClass", "TooManyFunctions")
object Fixtures {
    const val MAX_POPULARITY = 100

    @Suppress("MagicNumber")
    val notFoundId = "a".repeat(22)

    const val userId = "34m1o83qloqkyzdt4z3qbveoy"
    const val userDisplayName = "Test"

    // map from artist ID to whether or not the test users is following the artist
    val followingArtists = listOf(
        "5HA5aLY3jJV7eimXWkRBBp" to true, // Epica
        "6pRi6EIPXz4QJEOEsBaA0m" to false, // Chris Tomlin
        "3hE8S8ohRErocpkY7uJW4a" to true, // Within Temptation
        "3YCKuqpv9nCsIhJ2v8SMix" to false, // Elevation Worship
        "2KaW48xlLnXC2v8tvyhWsa" to true, // Amaranthe
    )

    // artists not being followed, but will be followed-and-unfollowed in tests
    val testFollowingArtists = listOf(
        "4kRllkt5ryNVBqFinVjBQZ", // Edenbridge
        "01DQQFGEOzbFugH5FcVAgI", // Amberian Dawn
    )

    // map from user ID to whether or not the test user is following the user
    val followingUsers = listOf(
        "djynth" to true,
        "bobbytonelli" to false,
    )

    // map from playlist ID to a map from user ID to whether the user is following that playlist
    val followingPlaylists = listOf(
        "5apAth0JL9APnjo62F93RN" to mapOf("djynth" to true, "luckyeights" to false),
        "6urDFlFQIDXPwXbfpdGUc0" to mapOf("djynth" to true, "1267916582" to true),
    )

    // a playlist not being followed, but will be followed-and-unfollowed in tests
    const val testFollowingPlaylist = "5apAth0JL9APnjo62F93RN"

    // sublist of recommended genres for the test user
    val recommendationGenres = listOf("metal")

    val albums = mapOf(
        AlbumProperties("1Z5Aw68hjd9e17izcGbLSQ", "Kikelet") to listOf(
            TrackProperties(
                id = "5p4S4vvrJO1wB5yxdOKcMl",
                name = "Búcsúzó",
                artistNames = setOf("Dalriada"),
                trackNumber = 1,
            ),
            TrackProperties(
                id = "49JyVkrdxsciZiFQ1amvUl",
                name = "Kikelet",
                artistNames = setOf("Dalriada"),
                trackNumber = 2,
            ),
            TrackProperties(
                id = "5bLICs3ZgqEe3MYFLCstdP",
                name = "Vándor-Fohász",
                artistNames = setOf("Dalriada"),
                trackNumber = 3,
            ),
            TrackProperties(
                id = "5ghCuL8wPjC0yESL7e4xQ5",
                name = "Táltosének",
                artistNames = setOf("Dalriada"),
                trackNumber = 4,
            ),
            TrackProperties(
                id = "4Aovdxm75RG9bM1daoGojO",
                name = "Néma Harangok",
                artistNames = setOf("Dalriada"),
                trackNumber = 5,
            ),
            TrackProperties(
                id = "40y0VEUdIvVXKsNOOnd1To",
                name = "Szentföld",
                artistNames = setOf("Dalriada"),
                trackNumber = 6,
            ),
            TrackProperties(
                id = "1ZowHVI8WFNjtfz7fqh7Rr",
                name = "Tűzhozó",
                artistNames = setOf("Dalriada"),
                trackNumber = 7,
            ),
            TrackProperties(
                id = "2FDPS2gYmtXmrIUNsR9qkc",
                name = "Tavasz Dala",
                artistNames = setOf("Dalriada"),
                trackNumber = 8,
            ),
            TrackProperties(
                id = "7gZJBbzUNQk36dYz7wrOqX",
                name = "Szondi Két Apródja II.rész",
                artistNames = setOf("Dalriada"),
                trackNumber = 9,
            ),
        ),

        AlbumProperties("7sDOBekGFHH2KfwW0vn6Me", "Arcane Astral Aeons") to listOf(
            TrackProperties(
                id = "3fsWHLlp3D9CzZv6YUo3pv",
                name = "In Styx Embrace",
                artistNames = setOf("Sirenia"),
                trackNumber = 1,
            ),
            TrackProperties(
                id = "6mHngSw9ywDzghL8YvsDnW",
                name = "Into the Night",
                artistNames = setOf("Sirenia"),
                trackNumber = 2,
            ),
            TrackProperties(
                id = "52UrdP9ByyzpzdxrKOkQhm",
                name = "Love Like Cyanide",
                artistNames = setOf("Sirenia"),
                trackNumber = 3,
            ),
            TrackProperties(
                id = "6ety9hMPEGoOso9jm01ogh",
                name = "Desire",
                artistNames = setOf("Sirenia"),
                trackNumber = 4,
            ),
            TrackProperties(
                id = "1Q5zahML4WCp6I2cOHoL4E",
                name = "Asphyxia",
                artistNames = setOf("Sirenia"),
                trackNumber = 5,
            ),
            TrackProperties(
                id = "037N6MX5jyhu5YXpyi7id2",
                name = "Queen of Lies",
                artistNames = setOf("Sirenia"),
                trackNumber = 6,
            ),
            TrackProperties(
                id = "4of1AAmOGeHQ91pV8hug5S",
                name = "Nos Heures Sombres",
                artistNames = setOf("Sirenia"),
                trackNumber = 7,
            ),
            TrackProperties(
                id = "0lzlpdsGRbHqfyMBcGNqSy",
                name = "The Voyage",
                artistNames = setOf("Sirenia"),
                trackNumber = 8,
            ),
            TrackProperties(
                id = "1w4ibHOzLSQNl7bYdB5GnF",
                name = "Aerodyne",
                artistNames = setOf("Sirenia"),
                trackNumber = 9,
            ),
            TrackProperties(
                id = "1BLrZ3IpeiKsPUjQDWyTsB",
                name = "The Twilight Hour",
                artistNames = setOf("Sirenia"),
                trackNumber = 10,
            ),
            TrackProperties(
                id = "1dQTItrEzPEoWj5k3s2Bsy",
                name = "Glowing Embers",
                artistNames = setOf("Sirenia"),
                trackNumber = 11,
            ),
            TrackProperties(
                id = "16JomutDWxRTgzeeFTYUdW",
                name = "Love Like Cyanide - Edit",
                artistNames = setOf("Sirenia"),
                trackNumber = 12,
            ),
        ),
    )

    val savedAlbums = listOf(
        AlbumProperties(
            id = "7sDOBekGFHH2KfwW0vn6Me",
            name = "Arcane Astral Aeons",
            totalTracks = 12,
            addedAt = "2021-02-21T05:15:47Z",
        ),
        AlbumProperties(
            id = "6N2Dn0OZ8KUDLsRqdToPcc",
            name = "The Unforgiving",
            totalTracks = 12,
            addedAt = "2021-02-21T05:16:10Z",
        ),
        AlbumProperties(
            id = "0KQNT6LnM05dUTr3slwnNJ",
            name = "Ephemeral",
            totalTracks = 12,
            addedAt = "2021-02-21T05:16:38Z",
        ),
    )

    // list of IDs of albums which are not in the user's saved albums
    val unsavedAlbums = listOf(
        "2SD5sTAWvPIXESCBah1kiu", // We Are the Catalyst - Elevation
        "3hW1TEeZRJ01XycQFABjj9", // Chopin - Piano Works
    )

    val artists = listOf(
        ArtistProperties(
            id = "7IxOJnsT8vXhTTzb6nlPOO",
            name = "Trees of Eternity",
            genres = listOf("atmospheric doom", "doom metal", "gaian doom", "gothic metal", "swedish doom metal"),
            albums = listOf(
                AlbumProperties("6sFhi9TivgwN6XzcEYcfAy", "Hour of the Nightingale", totalTracks = 10),
            ),
        ),

        ArtistProperties(
            id = "7lY8eBGG8mYelACRSlrpVr",
            name = "The Luna Sequence",
            genres = listOf("future rock"),
            albums = listOf(
                AlbumProperties("2xP1saNrb2En9IlVLycThQ", "Fearful Shepherds Hunt Their Sheep"),
                AlbumProperties("4aZJpsDmdhqdcoItwz13a5", "The Day the Curse Grew Stronger"),
                AlbumProperties("26CHBY1QH7PrZBXC2SBxaL", "This is Bloodlust"),
                AlbumProperties("3k883vMgteA5mJ6CFUAYL0", "They Follow You Home"),
                AlbumProperties(
                    "7qoxxvAcDhkYZ606Tx1bkx",
                    "Darkness Leaves Nowhere to Go",
                    albumType = SpotifyAlbum.Type.SINGLE,
                ),
                AlbumProperties("4eFzoRUFNILC8rOtCcYGaS", "Persona", albumType = SpotifyAlbum.Type.SINGLE),
                AlbumProperties("2iXHYo2oHrSET4ynAAODQi", "After Sunfall", albumType = SpotifyAlbum.Type.SINGLE),
                AlbumProperties("05rKXWgCCArwiIgqRgdj6h", "Underneath", albumType = SpotifyAlbum.Type.SINGLE),
                AlbumProperties(
                    "7uxbvtafrdoShwwah2OKLX",
                    "Erasus (FiXT Remix Compilation)",
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
                AlbumProperties(
                    "3JjPyBKbUXK20iJGnp7vgc",
                    "Wish Upon A Blackstar (Remix Contest Compilation)",
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
                AlbumProperties("4TIsfEUceYAfABg01yq0jV", "Around The World", albumType = SpotifyAlbum.Type.SINGLE),
                AlbumProperties(
                    "6w1FseZ89KHZY5x8mXH6kJ",
                    "Debut Remixes Vol. 02",
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
                AlbumProperties(
                    "2uXrzdhd7MlzKndZd2DRxk",
                    "Worldless (The FiXT Remixes)",
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
            ),
        ),

        ArtistProperties(
            id = "766wIvoqqGrjRDnExOjJls",
            name = "Thirteen Senses",
            genres = listOf("cornwall indie", "piano rock"),
            albums = listOf(
                AlbumProperties(
                    id = "5P7GIlX83brISe8k0XQQL1",
                    name = "A Strange Encounter",
                    totalTracks = 10,
                    albumType = SpotifyAlbum.Type.ALBUM,
                ),
                AlbumProperties(
                    id = "148kHVSDW2cwyAnmOUnSsm",
                    name = "Crystal Sounds",
                    totalTracks = 10,
                    albumType = SpotifyAlbum.Type.ALBUM,
                ),
                AlbumProperties(
                    id = "1v0kIX9QOYJhmbixRoWpeY",
                    name = "Contact",
                    totalTracks = 10,
                    albumType = SpotifyAlbum.Type.ALBUM,
                ),
                AlbumProperties(
                    id = "7JSZOMOVibNSpGWSfcgqcN",
                    name = "The Invitation",
                    totalTracks = 12,
                    albumType = SpotifyAlbum.Type.ALBUM,
                ),
                AlbumProperties(
                    id = "2Ea6nI5gRsy7QXCYFihMEk",
                    name = "Into The Fire (Acoustic)",
                    totalTracks = 1,
                    albumType = SpotifyAlbum.Type.SINGLE,
                ),
                AlbumProperties(
                    id = "3dXyenZqQJOktXiLRBXU55",
                    name = "Home",
                    totalTracks = 2,
                    albumType = SpotifyAlbum.Type.SINGLE,
                ),
                AlbumProperties(
                    id = "3CcltJjnlBarLBrnbU7Dgg",
                    name = "The Loneliest Star",
                    totalTracks = 2,
                    albumType = SpotifyAlbum.Type.SINGLE,
                ),
                AlbumProperties(
                    id = "75Rh0bIPEP7IJMUzpGo4os",
                    name = "Into The Fire (Cicada Remix)",
                    totalTracks = 1,
                    albumType = SpotifyAlbum.Type.SINGLE,
                ),
                AlbumProperties(
                    id = "0NHF6ViRN7Bv55QVwZmfGR",
                    name = "Rock Chillout",
                    totalTracks = 31,
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
                AlbumProperties(
                    id = "3ZCqORuVBN89Do4oYVDVnt",
                    name = "Chill Rock",
                    totalTracks = 30,
                    albumType = SpotifyAlbum.Type.COMPILATION,
                ),
            ),
        ),
    )

    val episodes = listOf(
        EpisodeProperties(
            id = "6XpUvPS3Y3iGIyguYrXUck",
            name = "#281 — Western Culture and Its Discontents",
            description = """
                Sam Harris speaks with Douglas Murray about his new book, “The War on the West.” They discuss the
                problem of hyper partisanship on the Left and Right, the primacy of culture, Hunter Biden’s laptop,
                the de-platforming of Trump and Alex Jones, the new religion of anti-racism, the problem of inequality,
                the 1619 Project, history of slavery, moral panics, the strange case of Michel Foucault, and other
                topics. If the Making Sense podcast logo in your player is BLACK, you can SUBSCRIBE to gain access to
                all full-length episodes at samharris.org/subscribe.   Learning how to train your mind is the single
                greatest investment you can make in life. That’s why Sam Harris created the Waking Up app. From rational
                mindfulness practice to lessons on some of life’s most important topics, join Sam as he demystifies the
                practice of meditation and explores the theory behind it."""
                .toSingleLine(),
            releaseDate = "2022-05-02",
            releaseDatePrecision = "day",
        ),
        EpisodeProperties(
            id = "61i9zd2aluBye0NiSf6NOh",
            name = "#1581 - J. Prince",
            description = """
                J. Prince is the CEO of Rap-A-Lot Records, author of The Art & Science of Respect, and founder of The
                Loyalty Collection, a limited collection of fine wines."""
                .toSingleLine(),
            releaseDate = "2020-12-18",
            releaseDatePrecision = "day",
        ),
    )

    val playlists = listOf(
        PlaylistProperties(
            id = "7aTpaSVlycAJyeU2SocEfA",
            name = "Favorites",
            description = "",
            public = true,
        ),
        PlaylistProperties(
            id = "5ajl0Scpq3FuNe2dgu2bMM",
            name = "Test Playlist",
            description = "test description",
            public = false,
            tracks = listOf(
                TrackProperties(
                    id = "6gtb1Oh7qRjobkH4jfJAnj",
                    name = "Grace",
                    artistNames = setOf("Apocalyptica", "HOTEI"),
                    trackNumber = 2,
                    addedBy = userId,
                    addedAt = "2021-02-11T06:20:38Z",
                ),
                TrackProperties(
                    id = "0hrNeGXIsFXCzGv27hDYlz",
                    name = "Chosen Time",
                    artistNames = setOf("Jeff Loomis"),
                    trackNumber = 8,
                    addedBy = userId,
                    addedAt = "2021-02-11T06:21:41Z",
                ),
            ),
        ),
    )

    val shows = listOf(
        ShowProperties(
            id = "1mNsuXfG95Lf76YQeVMuo1",
            name = "StarTalk Radio",
            saved = false,
            description = """
                Science, pop culture, and comedy collide on StarTalk Radio! Neil deGrasse Tyson, astrophysicist and
                Director of New York's Hayden Planetarium, and his comic co-hosts, guest celebrities, and scientific
                experts explore astronomy, physics, and everything else there is to know about life in the universe. New
                episodes premiere Tuesdays. Keep Looking Up!
                """
                .toSingleLine(),
        ),
        ShowProperties(
            id = "2mTUnDkuKUkhiueKcVWoP0",
            name = "Up First",
            saved = true,
            addedAt = "2021-02-21T06:12:22Z",
            description = """
                NPR's Up First is the news you need to start your day. The three biggest stories of the day, with
                reporting and analysis from NPR News — in 10 minutes. Available weekdays by 6 a.m. ET, with hosts Leila
                Fadel, Steve Inskeep, Michel Martin and A Martinez. Also available on Saturdays by 8 a.m. ET, with
                Ayesha Rascoe and Scott Simon. On Sundays, hear a longer exploration behind the headlines with Rachel
                Martin, available by 8 a.m. ET. Subscribe and listen, then support your local NPR station at
                donate.npr.org.Support NPR's reporting by subscribing to Up First+ and unlock sponsor-free listening.
                Learn more at plus.npr.org/upfirst
                """
                .toSingleLine(),
        ),
    )

    val tracks = listOf(
        TrackProperties(
            id = "1rBhSseb3q55BJdTdxuSf4",
            name = "Walk Unafraid",
            artistNames = setOf("R.E.M."),
            trackNumber = 9,
        ),

        TrackProperties(
            id = "4d5ft4QeC3GuHBwPGLnLc8",
            name = "Help",
            artistNames = setOf("Thaehan"),
            trackNumber = 13,
        ),

        TrackProperties(
            id = "7JyeTAshXQRlFSLJmgr9E6",
            name = "Nausicaa Requiem (Nausicaa Of The Valley Of The Wind)",
            artistNames = setOf("Imaginary Flying Machines", "Neroargento", "Yoko Hallelujah"),
            trackNumber = 12,
        ),

        TrackProperties(
            id = "3Or10XF8LCimAlD8k4TmCn",
            name = "We Believe",
            artistNames = setOf("Red Hot Chili Peppers"),
            trackNumber = 12,
            discNumber = 2,
        ),

        TrackProperties(
            id = "3I4qPl71TjjB8cXT2QlRb5",
            name = "Delusion",
            artistNames = setOf("We Are the Catalyst"),
            trackNumber = 1,
            explicit = true,
        ),
    )

    val savedTracks = listOf(
        TrackProperties(
            id = "6mgsDKeO6A8vZs128vg98R",
            name = "That's What The Wise Lady Said",
            artistNames = setOf("Angtoria"),
            trackNumber = 12,
            addedAt = "2021-02-21T05:52:14Z",
        ),
        TrackProperties(
            id = "16Z7RW76CpuaewB1U8H4cn",
            name = "Wheel of Time",
            artistNames = setOf("Blind Guardian"),
            trackNumber = 10,
            addedAt = "2021-02-21T05:52:57Z",
        ),
        TrackProperties(
            id = "0zvWXDMgXSBoOkZ3XKPGC6",
            name = "Greensleeves",
            artistNames = setOf("Blackmore's Night"),
            trackNumber = 14,
            addedAt = "2021-02-21T05:52:35Z",
        ),
    )

    val unsavedTracks = listOf(
        "1T8IRUJBga0JXioJZvxjBR", // DEUTSCHLAND by Rammstein
        "2MSgFefjK0T7Iwjvr3OKqV", // Chopin: Nocturne No. 20 in C-Sharp Minor, Op. Posth.
    )

    /**
     * Converts this multi-line string to a single-line one, with the indents and newlines removed (and replaced with
     * single spaces).
     */
    private fun String.toSingleLine(): String {
        return this.trimIndent().trim('\n').replace('\n', ' ')
    }
}
