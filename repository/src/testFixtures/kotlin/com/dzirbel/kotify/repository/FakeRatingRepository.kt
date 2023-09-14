package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.rating.RatingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeRatingRepository(
    ratings: Map<String, Rating> = emptyMap(),
    albumRatings: Map<String, AverageRating> = emptyMap(),
    artistRatings: Map<String, AverageRating> = emptyMap(),
) : RatingRepository {
    override val log = FakeLog<Repository.LogData>()

    private val ratings = ratings.toMutableMap()
    private val albumRatings = albumRatings.toMutableMap()
    private val artistRatings = artistRatings.toMutableMap()

    override fun ratingStateOf(id: String): StateFlow<Rating?> {
        return MutableStateFlow(ratings[id])
    }

    override fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>> {
        return ids.map { ratingStateOf(it) }
    }

    override fun averageRatingStateOf(ids: Iterable<String>): StateFlow<AverageRating> {
        return MutableStateFlow(AverageRating(ids.map { ratings[it] }))
    }

    override fun averageRatingStateOfArtist(artistId: String, scope: CoroutineScope): StateFlow<AverageRating> {
        return MutableStateFlow(artistRatings[artistId] ?: AverageRating.empty)
    }

    override fun averageRatingStateOfAlbum(albumId: String, scope: CoroutineScope): StateFlow<AverageRating> {
        return MutableStateFlow(albumRatings[albumId] ?: AverageRating.empty)
    }

    override fun rate(id: String, rating: Rating?) {
        if (rating == null) ratings.remove(id) else ratings[id] = rating
    }

    fun setArtistAverageRating(artistId: String, averageRating: AverageRating) {
        artistRatings[artistId] = averageRating
    }

    fun setAlbumAverageRating(albumId: String, averageRating: AverageRating) {
        albumRatings[albumId] = averageRating
    }
}
