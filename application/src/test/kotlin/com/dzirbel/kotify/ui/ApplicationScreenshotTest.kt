package com.dzirbel.kotify.ui

import androidx.compose.ui.unit.Density
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.AuthenticationState
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyImage
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.repository.FakeArtistRepository
import com.dzirbel.kotify.repository.FakePlayer
import com.dzirbel.kotify.repository.FakePlaylistRepository
import com.dzirbel.kotify.repository.FakeRatingRepository
import com.dzirbel.kotify.repository.FakeSavedAlbumRepository
import com.dzirbel.kotify.repository.FakeSavedArtistRepository
import com.dzirbel.kotify.repository.FakeSavedPlaylistRepository
import com.dzirbel.kotify.repository.FakeSavedTrackRepository
import com.dzirbel.kotify.repository.FakeUserRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.player.TrackPosition
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.put
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.ui.page.FakeImageViewModel
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.MockedTimeExtension
import com.dzirbel.kotify.util.collections.zipEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockedTimeExtension::class)
class ApplicationScreenshotTest {
    @Test
    fun test() {
        Application.setup()

        val artistRepository = FakeArtistRepository()
        val savedArtistRepository = FakeSavedArtistRepository()

        val playlistRepository = FakePlaylistRepository()
        val savedPlaylistRepository = FakeSavedPlaylistRepository()

        val savedTrackRepository = FakeSavedTrackRepository()
        val savedAlbumRepository = FakeSavedAlbumRepository()

        val ratingRepository = FakeRatingRepository()

        setupPlaylists(playlistRepository, savedPlaylistRepository)
        setupArtists(artistRepository, savedArtistRepository, ratingRepository)

        screenshotTest(
            filename = "application",
            configurations = listOf(KotifyColors.DARK, KotifyColors.LIGHT),
            windowWidth = 1920,
            windowHeight = 1080,
            windowDensity = Density(density = 0.65f, fontScale = 1.25f),
            onConfiguration = { colors -> Settings.colors = colors },
        ) {
            ProvideFakeRepositories(
                artistRepository = artistRepository,
                player = setupPlayer(ratingRepository, savedTrackRepository, savedAlbumRepository),
                playlistRepository = playlistRepository,
                ratingRepository = ratingRepository,
                savedAlbumRepository = savedAlbumRepository,
                savedArtistRepository = savedArtistRepository,
                savedPlaylistRepository = savedPlaylistRepository,
                savedTrackRepository = savedTrackRepository,
                userRepository = setupCurrentUser(),
            ) {
                Root(authenticationState = AuthenticationState.AUTHENTICATED)
            }
        }
    }

    private fun setupPlaylists(
        playlistRepository: FakePlaylistRepository,
        savedPlaylistRepository: FakeSavedPlaylistRepository,
    ) {
        val playlistIds = List(playlistNames.size) { "$it" }
        savedPlaylistRepository.setSaved(playlistIds)
        playlistIds.zipEach(playlistNames) { id, name ->
            playlistRepository.put(PlaylistViewModel(id = id, name = name, ownerId = "owner"))
        }
    }

    private fun setupArtists(
        artistRepository: FakeArtistRepository,
        savedArtistRepository: FakeSavedArtistRepository,
        ratingRepository: FakeRatingRepository,
    ) {
        var genericImageCount = 1
        artists.forEachIndexed { index, artist ->
            val id = artist.id ?: "$index"

            savedArtistRepository.save(id)

            artistRepository.put(
                ArtistViewModel(
                    id = id,
                    name = artist.name,
                    uri = "artist-$id",
                    images = FakeImageViewModel.fromFile(artist.imageName ?: "generic/${genericImageCount++}.png"),
                ),
            )

            ratingRepository.setArtistAverageRating(id, artist.ratings ?: ArtistInfo.generateRatings(id = id))
        }
    }

    private fun setupCurrentUser(): UserRepository {
        return FakeUserRepository(
            currentUser = UserViewModel(
                id = "kotify",
                name = "Kotify",
                images = FakeImageViewModel.fromFile("kotify.png"),
            ),
        )
    }

    private fun setupPlayer(
        ratingRepository: FakeRatingRepository,
        savedTrackRepository: FakeSavedTrackRepository,
        savedAlbumRepository: FakeSavedAlbumRepository,
    ): Player {
        val track = FullSpotifyTrack(
            id = "playing-track-id",
            name = "Streetcar Symphony",
            popularity = 0,
            trackNumber = 0,
            artists = listOf(
                SimplifiedSpotifyArtist(
                    id = playingArtist.id,
                    name = playingArtist.name,
                    externalUrls = SpotifyExternalUrl(),
                    type = "artist",
                ),
            ),
            discNumber = 1,
            durationMs = (4.minutes + 20.seconds).inWholeMilliseconds,
            explicit = false,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            isLocal = false,
            type = "track",
            uri = "uri",
            externalIds = SpotifyExternalId(),
            album = SimplifiedSpotifyAlbum(
                id = "playing-album-id",
                name = "Bangers Only",
                images = listOf(SpotifyImage(url = "kotify://playing-album")),
                externalUrls = emptyMap(),
                type = "album",
                artists = emptyList(),
            ),
        )

        val device = SpotifyPlaybackDevice(
            id = "device",
            name = "BART BeatBox",
            type = "smartphone",
            isActive = true,
            isRestricted = false,
            volumePercent = 80,
            isPrivateSession = false,
        )

        val positionMs = (1.minutes + 9.seconds).inWholeMilliseconds

        ratingRepository.rate(track.id, Rating(Rating.DEFAULT_MAX_RATING))
        savedTrackRepository.save(track.id)
        savedAlbumRepository.save("playing-album-id")

        SpotifyImageCache.set("kotify://playing-album", File("src/test/resources/pta1.png"))

        return FakePlayer(
            playable = true,
            playing = true,
            currentlyPlayingType = SpotifyPlayingType.TRACK,
            repeatMode = SpotifyRepeatMode.OFF,
            shuffling = false,
            currentItem = track,
            trackPosition = TrackPosition.Fetched(
                fetchedTimestamp = CurrentTime.millis,
                fetchedPositionMs = positionMs.toInt(),
                playing = false,
            ),
            currentDevice = device,
            volume = 80,
        )
    }

    private data class ArtistInfo(
        val name: String,
        val id: String? = null,
        val ratings: List<Rating>? = null,
        val imageName: String? = null,
    ) {
        companion object {
            fun generateRatings(id: String): List<Rating> {
                val random = Random(seed = id.hashCode().toLong())
                val numRatings = random.asJavaRandom().nextGaussian(10.0, 7.5).roundToInt().coerceAtLeast(0)
                val averageRating = random.asJavaRandom().nextGaussian(6.5, 2.0).coerceIn(2.0..9.0)
                return generateRatings(random = random, numRatings = numRatings, averageRating = averageRating)
            }

            fun generateRatings(random: Random = Random(0), numRatings: Int, averageRating: Double): List<Rating> {
                return List(numRatings) {
                    Rating(
                        rating = random
                            .asJavaRandom()
                            .nextGaussian(averageRating, 0.5)
                            .roundToInt()
                            .coerceIn(1..Rating.DEFAULT_MAX_RATING),
                        rateTime = Instant.EPOCH,
                    )
                }
            }
        }
    }

    companion object {
        private val playingArtist = ArtistInfo(
            name = "Public Transit Aficionados",
            id = "pta",
            ratings = ArtistInfo.generateRatings(numRatings = 18, averageRating = 9.7),
            imageName = "pta2.png",
        )

        private val artists = listOf(
            playingArtist,
            ArtistInfo(
                name = "Big Phil and the Boys",
                ratings = ArtistInfo.generateRatings(numRatings = 3, averageRating = 8.2),
                imageName = "bigphil.png",
            ),
            ArtistInfo(
                name = "A Bad Ghost Cover Band",
                ratings = ArtistInfo.generateRatings(numRatings = 1, averageRating = 1.0),
            ),
            ArtistInfo(
                name = "Among Thieves",
                ratings = emptyList(),
                imageName = "among-thieves.jpg",
            ),

            ArtistInfo(name = "The Electric Dreamweavers"),
            ArtistInfo(name = "Velvet Thunderstrike"),
            ArtistInfo(name = "Cosmic Echo Ensemble"),
            ArtistInfo(name = "The Neon Nomads"),
            ArtistInfo(name = "Stellar Serengeti"),
            ArtistInfo(name = "Psychedelic Stardust Revival"),
            ArtistInfo(name = "Groove Wizards"),
            ArtistInfo(name = "The Time-Traveling Troubadours"),
            ArtistInfo(name = "Celestial Sirens"),
            ArtistInfo(name = "Funky Space Cadets"),
            ArtistInfo(name = "Midnight Mirage"),
            ArtistInfo(name = "The Whimsical Wailers"),
            ArtistInfo(name = "Neon Jungle Funk"),
            ArtistInfo(name = "Captain Quasar and the Nebula Navigators"),
            ArtistInfo(name = "Lunar Lullaby Syndicate"),
            ArtistInfo(name = "The Quantum Groove Experiment"),
            ArtistInfo(name = "Galactic Gypsies"),
            ArtistInfo(name = "Solar Soul Architects"),
            ArtistInfo(name = "The Dreamcatcher Collective"),
            ArtistInfo(name = "Electric Oasis Odyssey"),
            ArtistInfo(name = "The Astral Alchemists"),
            ArtistInfo(name = "Cosmic Carnival Crew"),
            ArtistInfo(name = "Timeless Troublemakers"),
            ArtistInfo(name = "Ethereal Echo Chambers"),
            ArtistInfo(name = "The Soundwave Shamans"),
            ArtistInfo(name = "Neo-Victorian Velvettones"),
            ArtistInfo(name = "Hyperspace Harmonics"),
            ArtistInfo(name = "Psychedelic Pantheon"),
            ArtistInfo(name = "The Interdimensional Minstrels"),
            ArtistInfo(name = "Neon Moonlight Masquerade"),
            ArtistInfo(name = "Quantum Quirk Symphony"),
            ArtistInfo(name = "Electric Eden Explorers"),
            ArtistInfo(name = "The Psychedelic Paradox"),
            ArtistInfo(name = "Astral Alibi Orchestra"),
            ArtistInfo(name = "Nebula Navigators"),
            ArtistInfo(name = "Fantasia Fusion Collective"),
            ArtistInfo(name = "The Sonic Dreamweavers"),
            ArtistInfo(name = "Solaris Serenaders"),
            ArtistInfo(name = "Harmonic Hologram Herders"),
            ArtistInfo(name = "The Midnight Metaphors"),
            ArtistInfo(name = "Celestial Circus Parade"),
            ArtistInfo(name = "Galactic Groove Pioneers"),
            ArtistInfo(name = "Echoes of Elysium"),
            ArtistInfo(name = "The Timeless Tunesmiths"),
            ArtistInfo(name = "Velvet Voyage Visionaries"),
            ArtistInfo(name = "Cosmic Caravan Conjurers"),
        )

        private val playlistNames = listOf(
            "Classic Hits",
            "New Releases",
            "Ultimate Mix",
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
            "Singer-Songwriter Stories",
            "Vintage Soul and Funk",
        )
    }
}
