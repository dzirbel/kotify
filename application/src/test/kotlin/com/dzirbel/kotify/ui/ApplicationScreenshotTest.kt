package com.dzirbel.kotify.ui

import androidx.compose.ui.unit.Density
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.AuthenticationState
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.mockArtistTracksNull
import com.dzirbel.kotify.repository.mockLibrary
import com.dzirbel.kotify.repository.mockRating
import com.dzirbel.kotify.repository.mockSaveState
import com.dzirbel.kotify.repository.mockStates
import com.dzirbel.kotify.repository.player.PlayerRepository
import com.dzirbel.kotify.repository.player.SkippingState
import com.dzirbel.kotify.repository.player.TrackPosition
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.collections.zipEach
import com.dzirbel.kotify.util.withMockedObjects
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ApplicationScreenshotTest {
    @Test
    fun test() {
        CurrentTime.mocked {
            Application.setup()
            withMockedObjects(
                ArtistRepository,
                ArtistTracksRepository,
                PlayerRepository,
                PlaylistRepository,
                SavedAlbumRepository,
                SavedArtistRepository,
                SavedPlaylistRepository,
                SavedTrackRepository,
                TrackRatingRepository,
                UserRepository,
            ) {
                mockCurrentUser()
                mockSavedPlaylists()
                mockSavedArtists()
                mockPlayer()

                screenshotTest(
                    filename = "application",
                    configurations = listOf(KotifyColors.DARK, KotifyColors.LIGHT),
                    windowWidth = 1920,
                    windowHeight = 1080,
                    windowDensity = Density(density = 0.65f, fontScale = 1.25f),
                    onConfiguration = { colors ->
                        Settings.colors = colors
                    },
                ) {
                    Root(authenticationState = AuthenticationState.AUTHENTICATED)
                }
            }
        }
    }

    private fun mockSavedPlaylists() {
        val playlistIds = List(playlistNames.size) { "$it" }
        SavedPlaylistRepository.mockLibrary(ids = playlistIds.toSet())
        PlaylistRepository.mockStates(
            ids = playlistIds,
            values = playlistIds.zip(playlistNames) { id, name ->
                mockk {
                    val viewModel = this
                    every { viewModel.id } returns id
                    every { viewModel.name } returns name
                    every { viewModel.uri } returns "uri"
                }
            },
        )
    }

    private fun mockSavedArtists() {
        val artistIds = List(artists.size) { "$it" }
        SavedArtistRepository.mockLibrary(ids = artistIds.toSet())
        ArtistRepository.mockStates(
            ids = artistIds,
            values = artistIds.zip(artists) { id, artist ->
                mockk {
                    val viewModel = this
                    every { viewModel.id } returns id
                    every { viewModel.name } returns artist.name
                    every { viewModel.uri } returns "uri"
                    every { imageUrlFor(any()) } returns LazyTransactionStateFlow("kotify://artist-$id")
                }
            },
        )

        var genericImageCount = 1
        artistIds.zipEach(artists) { id, artist ->
            val imageFilename = artist.imageName ?: "generic/${genericImageCount++}.png"
            SpotifyImageCache.set("kotify://artist-$id", File("src/test/resources/$imageFilename"))

            val ratings = artist.ratings ?: ArtistInfo.generateRatings(id = id)
            every { TrackRatingRepository.averageRatingStateOfArtist(id, any()) } returns
                MutableStateFlow(AverageRating(ratings))
        }

        ArtistTracksRepository.mockArtistTracksNull()
    }

    private fun mockCurrentUser() {
        val user: UserViewModel = mockk {
            every { name } returns "Kotify"
            every { id } returns "kotify"
            every { imageUrlFor(any()) } returns LazyTransactionStateFlow("kotify://user")
        }
        every { UserRepository.currentUserId } returns MutableStateFlow("kotify")
        every { UserRepository.currentUser } returns
            MutableStateFlow(CacheState.Loaded(user, CurrentTime.instant))

        SpotifyImageCache.set("kotify://user", File("src/test/resources/kotify.png"))
    }

    private fun mockPlayer() {
        val track: FullSpotifyTrack = mockk {
            every { id } returns "playing-track-id"
            every { durationMs } returns (4.minutes + 20.seconds).inWholeMilliseconds
            every { name } returns "Streetcar Symphony"
            every { artists } returns listOf(
                mockk {
                    every { name } returns "Public Transit Aficionados"
                    every { id } returns "playing-artist-id"
                },
            )
            every { album } returns mockk {
                every { id } returns "playing-album-id"
                every { name } returns "Bangers Only"
                every { images } returns listOf(
                    mockk {
                        every { url } returns "kotify://playing-album"
                    },
                )
            }
        }
        val positionMs = (1.minutes + 9.seconds).inWholeMilliseconds
        val device: SpotifyPlaybackDevice = mockk {
            every { id } returns "device"
            every { name } returns "BART BeatBox"
            every { type } returns "smartphone"
        }

        every { PlayerRepository.refreshingPlayback } returns MutableStateFlow(false)
        every { PlayerRepository.refreshingTrack } returns MutableStateFlow(false)
        every { PlayerRepository.refreshingDevices } returns MutableStateFlow(false)
        every { PlayerRepository.playable } returns MutableStateFlow(true)
        every { PlayerRepository.playing } returns MutableStateFlow(ToggleableState.Set(true))
        every { PlayerRepository.playbackContextUri } returns MutableStateFlow(null)
        every { PlayerRepository.currentlyPlayingType } returns MutableStateFlow(SpotifyPlayingType.TRACK)
        every { PlayerRepository.skipping } returns MutableStateFlow(SkippingState.NOT_SKIPPING)
        every { PlayerRepository.repeatMode } returns MutableStateFlow(ToggleableState.Set(SpotifyRepeatMode.OFF))
        every { PlayerRepository.shuffling } returns MutableStateFlow(ToggleableState.Set(false))
        every { PlayerRepository.currentItem } returns MutableStateFlow(track)
        every { PlayerRepository.trackPosition } returns MutableStateFlow(
            TrackPosition.Fetched(
                fetchedTimestamp = CurrentTime.millis,
                fetchedPositionMs = positionMs.toInt(),
                playing = false,
            ),
        )
        every { PlayerRepository.currentDevice } returns MutableStateFlow(device)
        every { PlayerRepository.availableDevices } returns MutableStateFlow(listOf(device))
        every { PlayerRepository.volume } returns MutableStateFlow(ToggleableState.Set(80))
        every { PlayerRepository.errors } returns MutableSharedFlow()

        TrackRatingRepository.mockRating(id = track.id, rating = Rating(10, rateTime = Instant.EPOCH))
        SavedTrackRepository.mockSaveState(id = track.id, saved = true)
        SavedArtistRepository.mockSaveState(id = "playing-artist-id", saved = true)
        SavedAlbumRepository.mockSaveState(id = "playing-album-id", saved = true)
        SpotifyImageCache.set("kotify://playing-album", File("src/test/resources/pta1.png"))
    }

    private data class ArtistInfo(
        val name: String,
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
        private val artists = listOf(
            ArtistInfo(
                name = "Public Transit Aficionados",
                ratings = ArtistInfo.generateRatings(numRatings = 18, averageRating = 9.7),
                imageName = "pta2.png",
            ),
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
