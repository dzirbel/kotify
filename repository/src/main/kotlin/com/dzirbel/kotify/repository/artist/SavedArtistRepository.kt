package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseSavedRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.util.coroutines.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

interface SavedArtistRepository : SavedRepository

class DatabaseSavedArtistRepository(
    scope: CoroutineScope,
    userRepository: UserRepository,
    private val artistRepository: ArtistRepository,
) :
    DatabaseSavedRepository<FullSpotifyArtist>(
        savedEntityTable = ArtistTable.SavedArtistsTable,
        scope = scope,
        userRepository = userRepository,
    ),
    SavedArtistRepository {

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        return ids.chunked(size = Spotify.MAX_LIMIT).flatMapParallel { chunk ->
            Spotify.Follow.isFollowing(type = "artist", ids = chunk)
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) {
            Spotify.Follow.follow(type = "artist", ids = ids)
        } else {
            Spotify.Follow.unfollow(type = "artist", ids = ids)
        }
    }

    override suspend fun fetchLibrary(): Iterable<FullSpotifyArtist> {
        return Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
            .asFlow { url -> Spotify.get<Spotify.Follow.ArtistsCursorPagingModel>(url).artists }
            .toList()
    }

    override fun convertToDB(savedNetworkType: FullSpotifyArtist, fetchTime: Instant): Pair<String, Instant?> {
        artistRepository.convertToDB(networkModel = savedNetworkType, fetchTime = fetchTime)
        return savedNetworkType.id to null
    }
}
