package com.dzirbel.kotify.repository.rating

import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.db.util.single
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.util.collections.zipEach
import com.dzirbel.kotify.util.coroutines.combineState
import com.dzirbel.kotify.util.coroutines.flatMapLatestIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

// TODO unit test
class DatabaseRatingRepository(
    private val userRepository: UserRepository,
    private val applicationScope: CoroutineScope,
    private val userSessionScope: CoroutineScope,
    private val artistTracksRepository: ArtistTracksRepository,
    private val albumTracksRepository: AlbumTracksRepository,
) : RatingRepository {

    private val states = SynchronizedWeakStateFlowMap<String, Rating>()

    // TODO use log
    private val mutableLog = MutableLog<Repository.LogData>(
        name = requireNotNull(this::class.qualifiedName).removeSuffix(".Companion").substringAfterLast('.'),
        scope = applicationScope,
    )

    override val log = mutableLog.asLog()

    override fun ratingStateOf(id: String): StateFlow<Rating?> {
        return states.getOrCreateStateFlow(key = id) {
            userSessionScope.launch {
                val rating = KotifyDatabase[DB.RATINGS].transaction("load last rating of track id $id") {
                    lastRatingOf(id = id)
                }
                states.updateValue(id, rating)
            }
        }
    }

    override fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>> {
        return states.getOrCreateStateFlows(keys = ids) { creations ->
            userSessionScope.launch {
                val createdIds = creations.keys.toList() // convert to list to ensure consistent order
                val ratings = KotifyDatabase[DB.RATINGS].transaction("load last ratings of ${createdIds.size} tracks") {
                    createdIds.map { lastRatingOf(id = it) }
                }
                createdIds.zipEach(ratings) { id, rating ->
                    states.updateValue(id, rating)
                }
            }
        }
    }

    override fun averageRatingStateOf(ids: Iterable<String>): StateFlow<AverageRating> {
        // could theoretically be optimized by skipping the ordering of the rating list by the order of ids, since that
        // is irrelevant to the average
        return ratingStatesOf(ids = ids).combineState { AverageRating(it.toList()) }
    }

    /**
     * Combines the [ArtistTracksRepository.artistTracksStatesOf] with [averageRatingStateOf] to produce a [StateFlow]
     * of the average rating of the tracks by the artist, with collection in [scope].
     */
    override fun averageRatingStateOfArtist(artistId: String, scope: CoroutineScope): StateFlow<AverageRating> {
        return artistTracksRepository.artistTracksStateOf(artistId = artistId)
            .flatMapLatestIn(scope) { trackIds ->
                trackIds?.let { averageRatingStateOf(ids = it) }
                    ?: MutableStateFlow(AverageRating.empty)
            }
    }

    /**
     * Combines the [AlbumTracksRepository] states with [averageRatingStateOf] to produce a [StateFlow] of the average
     * rating of the tracks on the album, with collection in [scope].
     */
    override fun averageRatingStateOfAlbum(albumId: String, scope: CoroutineScope): StateFlow<AverageRating> {
        return albumTracksRepository.stateOf(id = albumId)
            .flatMapLatestIn(scope) { tracks ->
                tracks?.cachedValue?.tracks
                    ?.map { it.id }
                    ?.let { averageRatingStateOf(ids = it) }
                    ?: MutableStateFlow(AverageRating.empty)
            }
    }

    override fun rate(id: String, rating: Rating?) {
        val userId = userRepository.requireCurrentUserId
        applicationScope.launch {
            // assumes the new rating is always newer than the most recent one in the DB (unlike old implementation)
            states.updateValue(id, rating)

            if (rating == null) {
                // TODO just add a null rating on top rather than clearing history?
                KotifyDatabase[DB.RATINGS].transaction("clear rating for track id $id") {
                    TrackRatingTable.deleteWhere { (track eq id) and (TrackRatingTable.userId eq userId) }
                }
            } else {
                KotifyDatabase[DB.RATINGS].transaction("add rating for track id $id") {
                    TrackRatingTable.insert { statement ->
                        statement[track] = id
                        statement[TrackRatingTable.rating] = rating.rating
                        statement[maxRating] = rating.maxRating
                        statement[rateTime] = rating.rateTime
                        statement[TrackRatingTable.userId] = userId
                    }
                }
            }
        }
    }

    private fun lastRatingOf(id: String, userId: String = userRepository.requireCurrentUserId): Rating? {
        return TrackRatingTable
            .single(
                column1 = TrackRatingTable.rating,
                column2 = TrackRatingTable.maxRating,
                column3 = TrackRatingTable.rateTime,
                where = { (TrackRatingTable.track eq id) and (TrackRatingTable.userId eq userId) },
                order = TrackRatingTable.rateTime to SortOrder.DESC,
            )
            ?.let { Rating(rating = it.first, maxRating = it.second, rateTime = it.third) }
    }
}
