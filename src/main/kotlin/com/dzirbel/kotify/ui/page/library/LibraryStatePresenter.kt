package com.dzirbel.kotify.ui.page.library

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.PlaylistTrackTable
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.update

class LibraryStatePresenter(scope: CoroutineScope) :
    Presenter<LibraryStatePresenter.ViewModel?, LibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        // pair artistId, artist? in case we have the ID cached but not artist
        val artists: List<Pair<String, Artist?>>?,
        val artistsUpdated: Long?,

        // pair albumId, album? in case we have the ID cached but not album
        val albums: List<Pair<String, Album?>>?,
        val albumsUpdated: Long?,

        val playlists: List<Pair<String, Playlist?>>?,
        val playlistsUpdated: Long?,

        val tracks: List<Pair<String, Track?>>?,
        val tracksUpdated: Long?,

        val ratedTracks: List<Pair<String, Track?>>,
        val trackRatings: Map<String, State<Rating?>>,

        val refreshingSavedArtists: Boolean = false,
        val refreshingArtists: Set<String> = emptySet(),

        val refreshingSavedAlbums: Boolean = false,

        val refreshingSavedTracks: Boolean = false,

        val refreshingSavedPlaylists: Boolean = false,
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedArtists : Event()
        object RefreshSavedAlbums : Event()
        object RefreshSavedTracks : Event()
        object RefreshSavedPlaylists : Event()

        object FetchMissingArtists : Event()
        object InvalidateArtists : Event()
        object FetchMissingArtistAlbums : Event()
        object InvalidateArtistAlbums : Event()

        object FetchMissingAlbums : Event()
        object InvalidateAlbums : Event()

        object FetchMissingTracks : Event()
        object InvalidateTracks : Event()

        object ClearAllRatings : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()

        object FetchMissingPlaylists : Event()
        object InvalidatePlaylists : Event()
        object FetchMissingPlaylistTracks : Event()
        object InvalidatePlaylistTracks : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val savedArtistIds = SavedArtistRepository.getLibraryCached()?.toList()
                val savedArtists = savedArtistIds?.let { ArtistRepository.getCached(ids = it) }

                val savedAlbumIds = SavedAlbumRepository.getLibraryCached()?.toList()
                val savedAlbums = savedAlbumIds?.let { AlbumRepository.getCached(ids = it) }

                val savedTracksIds = SavedTrackRepository.getLibraryCached()?.toList()
                val savedTracks = savedTracksIds?.let { TrackRepository.getCached(ids = it) }

                val savedPlaylistIds = SavedPlaylistRepository.getLibraryCached()?.toList()
                val savedPlaylists = savedPlaylistIds?.let { PlaylistRepository.getCached(ids = it) }
                KotifyDatabase.transaction {
                    savedPlaylists?.onEach { it?.tracks?.loadToCache() }
                }

                val ratedTrackIds = TrackRatingRepository.ratedEntities().toList()
                val ratedTracks = TrackRepository.get(ids = ratedTrackIds)

                val trackRatings = ratedTrackIds.zipToMap(TrackRatingRepository.ratingStates(ids = ratedTrackIds))

                val state = ViewModel(
                    artists = savedArtistIds?.zip(savedArtists!!),
                    artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli(),
                    albums = savedAlbumIds?.zip(savedAlbums!!),
                    albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli(),
                    playlists = savedPlaylistIds?.zip(savedPlaylists!!),
                    playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli(),
                    tracks = savedTracksIds?.zip(savedTracks!!),
                    tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli(),
                    ratedTracks = ratedTrackIds.zip(ratedTracks),
                    trackRatings = trackRatings,
                )

                mutateState { state }
            }

            Event.RefreshSavedArtists -> {
                mutateState { it?.copy(refreshingSavedArtists = true) }

                SavedArtistRepository.invalidateLibrary()

                val artistIds = SavedArtistRepository.getLibrary().toList()
                val artists = ArtistRepository.getCached(ids = artistIds)
                val artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        artists = artistIds.zip(artists),
                        artistsUpdated = artistsUpdated,
                        refreshingSavedArtists = false,
                    )
                }
            }

            Event.RefreshSavedAlbums -> {
                mutateState { it?.copy(refreshingSavedAlbums = true) }

                SavedAlbumRepository.invalidateLibrary()

                val albumIds = SavedAlbumRepository.getLibrary().toList()
                val albums = AlbumRepository.getCached(ids = albumIds)
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        albums = albumIds.zip(albums),
                        albumsUpdated = albumsUpdated,
                        refreshingSavedAlbums = false,
                    )
                }
            }

            Event.RefreshSavedTracks -> {
                mutateState { it?.copy(refreshingSavedTracks = true) }

                SavedTrackRepository.invalidateLibrary()

                val trackIds = SavedTrackRepository.getLibrary().toList()
                val tracks = TrackRepository.getCached(ids = trackIds)
                val tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        tracks = trackIds.zip(tracks),
                        tracksUpdated = tracksUpdated,
                        refreshingSavedTracks = false
                    )
                }
            }

            Event.RefreshSavedPlaylists -> {
                mutateState { it?.copy(refreshingSavedPlaylists = true) }

                SavedPlaylistRepository.invalidateLibrary()

                val playlistIds = SavedPlaylistRepository.getLibrary().toList()
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }
                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        playlists = playlistIds.zip(playlists),
                        playlistsUpdated = playlistsUpdated,
                        refreshingSavedPlaylists = false
                    )
                }
            }

            Event.FetchMissingArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = ArtistRepository.getFull(ids = artistIds)

                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.InvalidateArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                ArtistRepository.invalidate(ids = artistIds)
                val artists = ArtistRepository.getCached(ids = artistIds)

                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.FetchMissingArtistAlbums -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val missingIds = KotifyDatabase.transaction {
                    Artist.find { ArtistTable.albumsFetched eq null }
                        .map { it.id.value }
                }
                    .filter { artistIds.contains(it) }

                missingIds
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { Artist.getAllAlbums(artistId = id) }
                    }
                    .collect()

                val artists = ArtistRepository.getCached(ids = artistIds)
                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.InvalidateArtistAlbums -> {
                KotifyDatabase.transaction {
                    AlbumTable.AlbumArtistTable.deleteAll()

                    ArtistTable.update(where = { Op.TRUE }) {
                        it[ArtistTable.albumsFetched] = null
                    }
                }

                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = ArtistRepository.getCached(ids = artistIds)
                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.FetchMissingAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                val albums = AlbumRepository.getFull(ids = albumIds)

                mutateState { it?.copy(albums = albumIds.zip(albums)) }
            }

            Event.InvalidateAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                AlbumRepository.invalidate(ids = albumIds)
                val albums = AlbumRepository.getCached(ids = albumIds)

                mutateState { it?.copy(albums = albumIds.zip(albums)) }
            }

            Event.FetchMissingTracks -> {
                val trackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()
                val tracks = TrackRepository.getFull(ids = trackIds)

                mutateState { it?.copy(tracks = trackIds.zip(tracks)) }
            }

            Event.InvalidateTracks -> {
                val trackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()
                TrackRepository.invalidate(ids = trackIds)
                val tracks = TrackRepository.getCached(ids = trackIds)

                mutateState { it?.copy(tracks = trackIds.zip(tracks)) }
            }

            Event.ClearAllRatings -> {
                TrackRatingRepository.clearAllRatings(userId = null)
                mutateState { it?.copy(ratedTracks = emptyList()) }
            }

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)

            Event.FetchMissingPlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.getFull(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.InvalidatePlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                PlaylistRepository.invalidate(ids = playlistIds)
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.FetchMissingPlaylistTracks -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.get(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                // TODO also fetch tracks for playlists not in the database at all
                val missingTracks = KotifyDatabase.transaction {
                    playlists.filter { it?.hasAllTracks == false }
                }

                missingTracks
                    .asFlow()
                    .flatMapMerge { playlist ->
                        flow<Unit> { playlist?.getAllTracks() }
                    }
                    .collect()

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.InvalidatePlaylistTracks -> {
                KotifyDatabase.transaction { PlaylistTrackTable.deleteAll() }

                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }
        }
    }
}
