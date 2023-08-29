package com.dzirbel.kotify.repository

import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.RatingRepository
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.user.UserRepository

/**
 * An array of all [Repository] implementations.
 */
val repositories: Array<Repository<*>> = arrayOf(
    AlbumRepository,
    AlbumTracksRepository,
    ArtistAlbumsRepository,
    ArtistRepository,
    PlaylistRepository,
    PlaylistTracksRepository,
    TrackRepository,
    UserRepository,
)

/**
 * An array of all [SavedRepository] implementations.
 */
val savedRepositories: Array<SavedRepository> = arrayOf(
    SavedAlbumRepository,
    SavedArtistRepository,
    SavedPlaylistRepository,
    SavedTrackRepository,
)

/**
 * An array of all [RatingRepository] implementations.
 */
val ratingRepositories: Array<RatingRepository> = arrayOf(
    TrackRatingRepository,
)

/**
 * A list of the [Log]s of all repositories (including saved and rating repositories), sorted by name.
 */
val repositoryLogs: List<Log<Log.Event>> = repositories.toList().plus(savedRepositories).plus(ratingRepositories)
    .map { it.log }
    .sortedBy { it.name }
