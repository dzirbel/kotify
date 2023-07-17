package com.dzirbel.kotify.repository2.artist

import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class SavedArtistRepository internal constructor(scope: CoroutineScope) :
    DatabaseSavedRepository<FullSpotifyArtist>(savedEntityTable = ArtistTable.SavedArtistsTable, scope = scope) {

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

    override fun convert(savedNetworkType: FullSpotifyArtist): Pair<String, Instant?> {
        ArtistRepository.convert(id = savedNetworkType.id, networkModel = savedNetworkType)
        return savedNetworkType.id to null
    }

    companion object : SavedArtistRepository(scope = Repository.userSessionScope)
}
