package com.dzirbel.kotify.ui

import com.dzirbel.kotify.Application
import com.dzirbel.kotify.AuthenticationState
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyImage
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.repository.FakeAlbumRepository
import com.dzirbel.kotify.repository.FakeAlbumTracksRepository
import com.dzirbel.kotify.repository.FakeArtistAlbumsRepository
import com.dzirbel.kotify.repository.FakeArtistRepository
import com.dzirbel.kotify.repository.FakePlayer
import com.dzirbel.kotify.repository.FakePlaylistRepository
import com.dzirbel.kotify.repository.FakePlaylistTracksRepository
import com.dzirbel.kotify.repository.FakeRatingRepository
import com.dzirbel.kotify.repository.FakeSavedAlbumRepository
import com.dzirbel.kotify.repository.FakeSavedArtistRepository
import com.dzirbel.kotify.repository.FakeSavedPlaylistRepository
import com.dzirbel.kotify.repository.FakeSavedTrackRepository
import com.dzirbel.kotify.repository.FakeUserRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumViewModel
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.player.TrackPosition
import com.dzirbel.kotify.repository.put
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.ui.page.FakeImageViewModel
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.page.playlist.PlaylistPage
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.MockedTimeExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockedTimeExtension::class)
class ApplicationScreenshotTest {
    private val artistRepository = FakeArtistRepository()
    private val savedArtistRepository = FakeSavedArtistRepository()
    private val playlistRepository = FakePlaylistRepository()
    private val savedPlaylistRepository = FakeSavedPlaylistRepository()
    private val savedTrackRepository = FakeSavedTrackRepository()
    private val savedAlbumRepository = FakeSavedAlbumRepository()
    private val ratingRepository = FakeRatingRepository()

    private val random = Random(0)

    @BeforeEach
    fun setup() {
        Application.setupProperties(debug = false)

        savedPlaylistRepository.setSaved(ApplicationFixtures.savedPlaylists.map { it.id })
        playlistRepository.put(ApplicationFixtures.savedPlaylists)
    }

    @Test
    fun artists() {
        savedArtistRepository.setSaved(ApplicationFixtures.savedArtists.map { it.id })
        artistRepository.put(ApplicationFixtures.savedArtists)
        for (artist in ApplicationFixtures.savedArtists) {
            ratingRepository.setArtistAverageRating(artist.id, ApplicationFixtures.ratingForArtist(artist.id, random))
        }

        applicationScreenshotTest("artists", ArtistsPage) {
            ProvideFakeRepositories(
                artistRepository = artistRepository,
                player = setupPlayer(),
                playlistRepository = playlistRepository,
                ratingRepository = ratingRepository,
                savedAlbumRepository = savedAlbumRepository,
                savedArtistRepository = savedArtistRepository,
                savedPlaylistRepository = savedPlaylistRepository,
                savedTrackRepository = savedTrackRepository,
                userRepository = FakeUserRepository(currentUser = ApplicationFixtures.user),
            ) {
                Root(authenticationState = AuthenticationState.AUTHENTICATED)
            }
        }
    }

    @Test
    fun playlist() {
        val playlist = ApplicationFixtures.undergroundJams
        val tracks = ApplicationFixtures.tracksForPlaylist(random)

        for (track in tracks) {
            val trackId = requireNotNull(track.track).id

            if (random.nextInt(10) != 0) {
                ratingRepository.rate(trackId, random.nextGaussianRating(mean = 7.5, stddev = 1.5))
            }

            savedTrackRepository.setSaved(trackId, random.nextInt(5) != 0)
        }

        val playlistTracksRepository = FakePlaylistTracksRepository(mapOf(playlist.id to tracks))

        applicationScreenshotTest("playlist", PlaylistPage(playlistId = playlist.id)) {
            ProvideFakeRepositories(
                artistRepository = artistRepository,
                player = setupPlayer(),
                playlistRepository = playlistRepository,
                ratingRepository = ratingRepository,
                savedAlbumRepository = savedAlbumRepository,
                savedArtistRepository = savedArtistRepository,
                savedPlaylistRepository = savedPlaylistRepository,
                savedTrackRepository = savedTrackRepository,
                playlistTracksRepository = playlistTracksRepository,
                userRepository = FakeUserRepository(currentUser = ApplicationFixtures.user),
            ) {
                Root(authenticationState = AuthenticationState.AUTHENTICATED)
            }
        }
    }

    @Test
    fun artist() {
        val random = Random(0)
        val artist = ApplicationFixtures.pta
        val albums = ApplicationFixtures.transitAlbums
            .take(15)
            .onEach { album ->
                savedAlbumRepository.setSaved(album.id, random.nextInt(10) != 0)
            }
            .mapIndexed { index, album ->
                album.copy(
                    images = FakeImageViewModel.fromFile("generic/${index + 1}.png"),
                    totalTracks = random.nextGaussian(mean = 10, stddev = 2, min = 1, max = 20).toInt(),
                )
            }
            .plus(ApplicationFixtures.bangersOnly)
            .onEach { album ->
                ratingRepository.setAlbumAverageRating(album.id, ApplicationFixtures.ratingForAlbum(album, random))
            }
        val artistAlbums = albums.map { ArtistAlbumViewModel(album = it, artist = artist) }

        artistRepository.put(artist)
        val artistAlbumsRepository = FakeArtistAlbumsRepository(mapOf(artist.id to artistAlbums))
        val albumRepository = FakeAlbumRepository(albums)

        applicationScreenshotTest("artist", ArtistPage(artistId = artist.id)) {
            ProvideFakeRepositories(
                artistRepository = artistRepository,
                player = setupPlayer(),
                playlistRepository = playlistRepository,
                ratingRepository = ratingRepository,
                savedAlbumRepository = savedAlbumRepository,
                savedArtistRepository = savedArtistRepository,
                savedPlaylistRepository = savedPlaylistRepository,
                savedTrackRepository = savedTrackRepository,
                artistAlbumsRepository = artistAlbumsRepository,
                albumRepository = albumRepository,
                userRepository = FakeUserRepository(currentUser = ApplicationFixtures.user),
            ) {
                Root(authenticationState = AuthenticationState.AUTHENTICATED)
            }
        }
    }

    @Test
    fun album() {
        val random = Random(0)
        val album = ApplicationFixtures.bangersOnly
        val ratings = mutableListOf<Rating>()
        val tracks = ApplicationFixtures.Names.transitTracks
            .take(requireNotNull(album.totalTracks) - 1)
            .mapIndexed { index, name ->
                TrackViewModel(
                    id = name,
                    name = name,
                    trackNumber = index + 2,
                    durationMs = random.nextTrackDurationMs(),
                    popularity = random.nextGaussian(mean = 80, stddev = 10, max = 100, min = 0).roundToInt(),
                    album = LazyTransactionStateFlow(album),
                    artists = LazyTransactionStateFlow(listOf(ApplicationFixtures.pta)),
                )
            }
            .onEach { track ->
                val rating = random.nextGaussianRating(mean = 9.3)
                ratingRepository.rate(track.id, rating)
                ratings.add(rating)

                savedTrackRepository.setSaved(track.id, random.nextInt(10) != 0)
            }
            .plus(
                ApplicationFixtures.streetcarSymphony.also {
                    ratings.add(Rating(Rating.DEFAULT_MAX_RATING))
                },
            )

        ratingRepository.setAlbumAverageRating(album.id, AverageRating(ratings))

        val albumRepository = FakeAlbumRepository(listOf(album))
        val albumTracksRepository = FakeAlbumTracksRepository(mapOf(album.id to tracks))

        applicationScreenshotTest("album", AlbumPage(albumId = album.id)) {
            ProvideFakeRepositories(
                artistRepository = artistRepository,
                player = setupPlayer(),
                playlistRepository = playlistRepository,
                ratingRepository = ratingRepository,
                savedAlbumRepository = savedAlbumRepository,
                savedArtistRepository = savedArtistRepository,
                savedPlaylistRepository = savedPlaylistRepository,
                savedTrackRepository = savedTrackRepository,
                albumRepository = albumRepository,
                albumTracksRepository = albumTracksRepository,
                userRepository = FakeUserRepository(currentUser = ApplicationFixtures.user),
            ) {
                Root(authenticationState = AuthenticationState.AUTHENTICATED)
            }
        }
    }

    private fun setupPlayer(playingTrack: TrackViewModel = ApplicationFixtures.streetcarSymphony): Player {
        val artists = requireNotNull(playingTrack.artists.value).map { artist ->
            SimplifiedSpotifyArtist(
                id = artist.id,
                name = artist.name,
                externalUrls = SpotifyExternalUrl(),
                type = "artist",
            )
        }

        val track = FullSpotifyTrack(
            id = playingTrack.id,
            name = playingTrack.name,
            popularity = playingTrack.popularity ?: 0,
            trackNumber = playingTrack.trackNumber,
            artists = artists,
            discNumber = 1,
            durationMs = playingTrack.durationMs,
            explicit = false,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            isLocal = false,
            type = "track",
            uri = playingTrack.uri.orEmpty(),
            externalIds = SpotifyExternalId(),
            album = requireNotNull(playingTrack.album.value).let { album ->
                SimplifiedSpotifyAlbum(
                    id = album.id,
                    name = album.name,
                    images = listOfNotNull(
                        (album.images as FakeImageViewModel).imageUrl?.let { SpotifyImage(url = it) },
                    ),
                    externalUrls = emptyMap(),
                    type = "album",
                    artists = artists,
                )
            },
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
        savedAlbumRepository.save(requireNotNull(playingTrack.album.value).id)
        for (artist in artists) { savedArtistRepository.save(requireNotNull(artist.id)) }

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
}
