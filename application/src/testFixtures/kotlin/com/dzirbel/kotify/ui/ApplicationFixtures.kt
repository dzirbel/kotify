package com.dzirbel.kotify.ui

import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.ui.page.FakeImageViewModel
import com.dzirbel.kotify.util.CurrentTime
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object ApplicationFixtures {
    val user = UserViewModel(id = "kotify", name = "Kotify", images = FakeImageViewModel.fromFile("kotify.png"))

    val pta = ArtistViewModel(
        id = "pta",
        name = "Public Transit Aficionados",
        uri = "artist:pta",
        images = FakeImageViewModel.fromFile("pta2.png"),
    )

    val savedArtists: List<ArtistViewModel> = listOf(
        pta,
        ArtistViewModel(
            id = "bigphil",
            name = "Big Phil and the Boys",
            uri = "artist:bigphil",
            images = FakeImageViewModel.fromFile("bigphil.png"),
        ),
        ArtistViewModel(
            id = "badghost",
            name = "A Bad Ghost Cover Band",
            uri = "artist:badghost",
            images = FakeImageViewModel.fromFile("generic/1.png"),
        ),
        ArtistViewModel(
            id = "among-thieves",
            name = "Among Thieves",
            uri = "artist:among-thieves",
            images = FakeImageViewModel.fromFile("among-thieves.jpg"),
        ),
    )
        .plus(
            Names.genericArtists.mapIndexed { index, name ->
                ArtistViewModel(
                    id = "artist-$index",
                    name = name,
                    uri = "artist-$index",
                    images = FakeImageViewModel.fromFile("generic/${index + 2}.png"),
                )
            },
        )

    val undergroundJams = PlaylistViewModel(
        id = "playlist-underground-jams",
        name = "Underground Jams",
        uri = "playlist:underground-jams",
        ownerId = user.id,
        followersTotal = 48954,
        totalTracks = 42,
        images = FakeImageViewModel.fromFile("underground_jams.png"),
    )

    val bangersOnly = AlbumViewModel(
        id = "playing-album",
        name = "Bangers Only",
        uri = "album:playing-album",
        totalTracks = 13,
        images = FakeImageViewModel.fromFile("pta1.png"),
        artists = LazyTransactionStateFlow(listOf(pta)),
        albumType = AlbumType.ALBUM,
    )

    val streetcarSymphony = TrackViewModel(
        id = "playing-track",
        name = "Streetcar Symphony",
        uri = "track:playing-track",
        trackNumber = 0,
        durationMs = (4.minutes + 20.seconds).inWholeMilliseconds,
        artists = LazyTransactionStateFlow(listOf(pta)),
        album = LazyTransactionStateFlow(bangersOnly),
    )

    val savedPlaylists: List<PlaylistViewModel> = Names.genericPlaylists.mapIndexed { index, name ->
        PlaylistViewModel(
            id = "playlist-$name", // use name as ID so the library will be sorted by name
            name = name,
            uri = "playlist:$index",
            ownerId = user.id,
            images = FakeImageViewModel(),
        )
    }
        .plus(undergroundJams)
        .sortedBy { it.name }

    val transitArtists: List<ArtistViewModel> = Names.transitArtists.mapIndexed { index, name ->
        ArtistViewModel(
            id = "artist-$index",
            name = name,
            uri = "artist:$index",
            images = FakeImageViewModel(),
        )
    }

    val transitAlbums: List<AlbumViewModel> = Names.transitAlbums.mapIndexed { index, name ->
        AlbumViewModel(
            id = "album-$index",
            name = name,
            uri = "album:$index",
            images = FakeImageViewModel(),
            albumType = AlbumType.ALBUM,
        )
    }

    fun ratingForArtist(artistId: String, random: Random): AverageRating {
        val ratings = when (artistId) {
            pta.id -> {
                List(18) { random.nextGaussianRating(mean = 9.7) }
            }

            "bigphil" -> {
                List(3) { random.nextGaussianRating(mean = 8.2) }
            }

            "badghost" -> {
                List(1) { random.nextGaussianRating(mean = 1.0) }
            }

            "among-thieves" -> {
                emptyList()
            }

            else -> {
                val numRatings = random.nextGaussian(mean = 10.0, stddev = 7.5, min = 0).roundToInt()
                val averageRating = random.nextGaussian(mean = 6.5, stddev = 2.0, min = 2.0, max = 9.0)
                List(numRatings) { random.nextGaussianRating(mean = averageRating) }
            }
        }

        return AverageRating(ratings)
    }

    fun ratingForAlbum(album: AlbumViewModel, random: Random): AverageRating {
        val ratings = if (album == bangersOnly) {
            List(requireNotNull(bangersOnly.totalTracks)) { random.nextGaussianRating(mean = 9.7) }
        } else {
            val numRatings = random.nextGaussian(
                mean = album.totalTracks?.let { it / 2 } ?: 10,
                stddev = 7.5,
                min = 0,
                max = album.totalTracks,
            )
            val averageRating = random.nextGaussian(mean = 6.5, stddev = 2.0, min = 2.0, max = 9.0)
            List(numRatings.roundToInt()) { random.nextGaussianRating(mean = averageRating) }
        }

        return AverageRating(ratings)
    }

    fun tracksForPlaylist(random: Random): List<PlaylistTrackViewModel> {
        return List(requireNotNull(undergroundJams.totalTracks)) { index ->
            val track = TrackViewModel(
                id = "$index",
                name = Names.transitTracks[index],
                trackNumber = 0,
                durationMs = random
                    .nextGaussian(mean = 4.minutes.inWholeMilliseconds, stddev = 1.minutes.inWholeMilliseconds)
                    .coerceAtLeast(0.0)
                    .toLong(),
                album = LazyTransactionStateFlow(transitAlbums.random(random)),
                artists = LazyTransactionStateFlow(listOf(transitArtists.random(random))),
                popularity = random.nextGaussian(mean = 70, stddev = 20, min = 0, max = 100).roundToInt(),
            )

            PlaylistTrackViewModel(
                track = track,
                indexOnPlaylist = index,
                addedAt = random.nextGaussian(mean = 50, stddev = 30, min = 0)
                    .let { daysAgo -> CurrentTime.instant.minus(daysAgo.days.toJavaDuration()) }
                    .toString(),
            )
        }
    }

    object Names {
        val genericArtists = listOf(
            "The Electric Dreamweavers",
            "Velvet Thunderstrike",
            "Cosmic Echo Ensemble",
            "The Neon Nomads",
            "Stellar Serengeti",
            "Psychedelic Stardust Revival",
            "Groove Wizards",
            "The Time-Traveling Troubadours",
            "Celestial Sirens",
            "Funky Space Cadets",
            "Midnight Mirage",
            "The Whimsical Wailers",
            "Neon Jungle Funk",
            "Captain Quasar and the Nebula Navigators",
            "Lunar Lullaby Syndicate",
            "The Quantum Groove Experiment",
            "Galactic Gypsies",
            "Solar Soul Architects",
            "The Dreamcatcher Collective",
            "Electric Oasis Odyssey",
            "The Astral Alchemists",
            "Cosmic Carnival Crew",
            "Timeless Troublemakers",
            "Ethereal Echo Chambers",
            "The Soundwave Shamans",
            "Neo-Victorian Velvettones",
            "Hyperspace Harmonics",
            "Psychedelic Pantheon",
            "The Interdimensional Minstrels",
            "Neon Moonlight Masquerade",
            "Quantum Quirk Symphony",
            "Electric Eden Explorers",
            "The Psychedelic Paradox",
            "Astral Alibi Orchestra",
            "Nebula Navigators",
            "Fantasia Fusion Collective",
            "The Sonic Dreamweavers",
            "Solaris Serenaders",
            "Harmonic Hologram Herders",
            "The Midnight Metaphors",
            "Celestial Circus Parade",
            "Galactic Groove Pioneers",
            "Echoes of Elysium",
            "The Timeless Tunesmiths",
            "Velvet Voyage Visionaries",
            "Cosmic Caravan Conjurers",
        )

        val transitArtists = listOf(
            "Echo Chamber Collective",
            "Velvet Undergroundvibes",
            "Subterranean Soundscapes",
            "Neon Street Syndicate",
            "Urban Echoes Project",
            "Secret Tunnel Troubadours",
            "Midnight Metro Muses",
            "Tunnel Visionaries",
            "Hidden Gem Harmonics",
            "Subway Serenaders",
            "Labyrinthine Beats",
            "Shadow City Soundtrack",
            "Cobblestone Crooners",
            "Subterranea Sensations",
            "Urban Jungle Groovers",
            "Mole People Melodies",
            "Rooftop Revolutionaries",
            "Forgotten Underground Icons",
            "Alleyway Artisans",
            "Concrete Jungle Rhythms",
            "Transit Tracks Collective",
            "Metro Melodic Masters",
            "Train Car Troubadours",
            "Bus Stop Balladeers",
            "Subway Symphony Society",
            "Tramline Tunesmiths",
            "Commute Cadence Crew",
            "Transit Groove Ensemble",
            "Streetcar Serenaders",
            "Ticket to Groovetown",
            "Platform Performers",
            "City Transit Trouvères",
            "Transit Beatcrafters",
            "Commuter's Chorus",
            "Route Rhythm Revival",
            "Station Serenade Set",
            "Cityscape Sound Sailors",
            "Underground Rhapsodists",
            "Sidewalk Syncopators",
            "Transit Harmony Collective",
        )

        val transitAlbums = listOf(
            "Urban Chronicles",
            "City Stories",
            "Downtown Diaries",
            "Street-Level Melodies",
            "Metropolis Memoirs",
            "Soundtrack of the Streets",
            "Cityscape Serenades",
            "Neon Nights",
            "Echoes of the Sidewalk",
            "Nocturnal Notes",
            "Concrete Dreams",
            "City Lights and Shadows",
            "The Commuter's Playlist",
            "Tales of the Night City",
            "Pavement Poetry",
            "Urban Echoes",
            "Soul of the City",
            "Metropolitan Melodies",
            "Street Songs and Stories",
            "Rhythms of the Asphalt",
            "City Soundscape",
            "In the Heart of the City",
            "Urban Groove Collective",
            "The City After Dark",
            "Metropolitan Musings",
            "Nights in Neon",
            "City Life Chronicles",
            "Avenue Anthems",
            "Sidewalk Serenades",
            "Late-Night Lullabies",
            "Concrete Jungle Jams",
            "City Sights and Sounds",
            "Streetside Stories",
            "Urbane Rhythms",
            "Urban Legends in Music",
            "City Beat Chronicles",
            "Soundtrack of the Urban Jungle",
            "Echoes of the Night City",
            "City Pulse",
            "Metropolis Reflections",
            "Streetcar Serenades",
            "Nightlife Notes",
            "City Stories in Sound",
            "Cityscape Chronicles",
            "Sounds of the Sidewalk",
            "Metropolitan Memoirs",
            "Avenue Anthems",
            "Urban Dreamscape",
            "The City Unplugged",
            "Echoes from the Streets",
        )

        val genericPlaylists = listOf(
            "Classic Hits",
            "New Releases",
            "Weekend Vibes",
            "Relaxation Station",
            "Workout Beats",
            "Late Night Jams",
            "Sunday Morning Tunes",
            "Drive Time Playlist",
            "Study Session Sounds",
            "Smooth Jazz Serenity",
            "90s Grunge Revival",
            "Hip-Hop Old School Legends",
            "Acoustic Folk Coffeehouse",
            "Indie Electronica Dreams",
            "Classic Rock Road Trip",
            "Salsa Sensations",
            "Motown Magic",
            "Blues Guitar Heroes",
            "Ambient Chillout Lounge",
            "Latin Jazz Fiesta",
            "Reggae Roots & Culture",
            "Classical Piano Delights",
            "Indie Rock Underground",
            "Soulful R&B Ballads",
            "Country BBQ Jams",
            "EDM Dancefloor Bangers",
            "Metalcore Mayhem",
            "Funky Groove Fusion",
            "Top 40 Pop Chart-Toppers",
            "Alternative Indie Vibes",
            "Bluegrass Bonanza",
            "Korean Hip-Hop Hotlist",
            "Psychedelic Rock Odyssey",
            "Tropical House Paradise",
            "Punk Rock Riot",
            "African Beats Adventure",
            "Chillstep Bliss",
            "Vintage Soul and Funk",
        )

        val transitTracks = listOf(
            "City Lights",
            "Neon Nights",
            "Sidewalk Serenade",
            "Metropolitan Melody",
            "Urban Echoes",
            "Street-Level Symphony",
            "Nocturnal Notes",
            "Downtown Diary",
            "Concrete Dreams",
            "Nighttime Nostalgia",
            "Avenue Anthem",
            "Cityscape Serenade",
            "Rhythms of the Asphalt",
            "Echoes of the Sidewalk",
            "Late-Night Lullaby",
            "Soul of the City",
            "Urban Sunrise",
            "Pavement Poetry",
            "The Commuter's Chorus",
            "City Soundtrack",
            "Metropolis Memoir",
            "Streetcar Sonata",
            "Neon Noir",
            "Dusk Till Dawn",
            "Urban Reverie",
            "Nightscape Serenade",
            "City Pulse",
            "Streetside Story",
            "Metropolitan Muse",
            "Echoes from the Alley",
            "Urban Legends",
            "Avenue Amplitude",
            "Cityscape Chronicles",
            "Nighttime Sonata",
            "Late-Night Reverie",
            "Concrete Jungle Jazz",
            "Sidewalk Serenade",
            "Metropolitan Muse",
            "City Beat",
            "Urban Poetry",
            "Neon Dreams",
            "Streetlight Serenade",
            "Nocturnal Narrative",
            "Avenue Alcove",
            "Cityscape Serenity",
            "Urban Solitude",
            "Nightlife Notes",
            "Downtown Daydream",
            "Metropolis Memory",
            "Echoes of the Night City",
        )
    }
}
