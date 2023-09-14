package com.dzirbel.kotify.repository.rating

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.log.Logging
import com.dzirbel.kotify.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Specifies the public API for repositories which manage [Rating] states (of tracks).
 */
@Stable
interface RatingRepository : Logging<Repository.LogData> {
    /**
     * Returns a live [StateFlow] reflecting the current [Rating] (for the current user) of the entity with the given
     * [id]; null if the current user has not rated it.
     */
    fun ratingStateOf(id: String): StateFlow<Rating?>

    /**
     * Returns a batch of live [StateFlow]s reflecting the current [Rating]s (for the current user) of the entities with
     * the given [ids]; null if the current user has not rated it.
     *
     * The returned list has the same size and is in the same order as [ids].
     */
    fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>>

    /**
     * Returns a live [StateFlow] reflecting the current [AverageRating] (for the current user) of the entities with the
     * given [ids].
     */
    fun averageRatingStateOf(ids: Iterable<String>): StateFlow<AverageRating>

    fun averageRatingStateOfArtist(artistId: String, scope: CoroutineScope): StateFlow<AverageRating>

    fun averageRatingStateOfAlbum(albumId: String, scope: CoroutineScope): StateFlow<AverageRating>

    /**
     * Asynchronously applies the given [rating] to the entity with the given [id].
     */
    fun rate(id: String, rating: Rating?)
}
