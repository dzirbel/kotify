package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Genre
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.util.sized
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseEntityRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.util.updateOrInsert
import com.dzirbel.kotify.util.coroutines.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

interface ArtistRepository : Repository<ArtistViewModel>, ConvertingRepository<Artist, SpotifyArtist>

class DatabaseArtistRepository(scope: CoroutineScope) :
    DatabaseEntityRepository<ArtistViewModel, Artist, SpotifyArtist>(entityClass = Artist, scope = scope),
    ArtistRepository {

    override suspend fun fetchFromRemote(id: String) = Spotify.Artists.getArtist(id = id)
    override suspend fun fetchFromRemote(ids: List<String>): List<SpotifyArtist?> {
        return ids.chunked(size = Spotify.MAX_LIMIT)
            .flatMapParallel { idsChunk -> Spotify.Artists.getArtists(ids = idsChunk) }
    }

    override fun convertToDB(id: String, networkModel: SpotifyArtist, fetchTime: Instant): Artist {
        return Artist.updateOrInsert(id = id, networkModel = networkModel, fetchTime = fetchTime) {
            if (networkModel is FullSpotifyArtist) {
                fullUpdatedTime = fetchTime

                popularity = networkModel.popularity
                followersTotal = networkModel.followers.total
                images = networkModel.images
                    .map { Image.findOrCreate(url = it.url, width = it.width, height = it.height) }
                    .sized()

                genres = networkModel.genres.map { Genre.findOrCreate(it) }.sized()
            }
        }
    }

    override fun convertToVM(databaseModel: Artist, fetchTime: Instant) = ArtistViewModel(databaseModel)
}
