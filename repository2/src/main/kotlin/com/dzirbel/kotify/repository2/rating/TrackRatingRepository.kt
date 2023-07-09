package com.dzirbel.kotify.repository2.rating

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.repository2.user.UserRepository
import com.dzirbel.kotify.repository2.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.util.combineState
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Max
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

// TODO unit test
object TrackRatingRepository : RatingRepository {
    private val states = SynchronizedWeakStateFlowMap<String, Rating>()

    override fun ratingStateOf(id: String): StateFlow<Rating?> {
        return states.getOrCreateStateFlow(key = id) {
            Repository.scope.launch {
                val rating = lastRatingOf(id = id)
                states.updateValue(id, rating)
            }
        }
    }

    override fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>> {
        return states.getOrCreateStateFlows(keys = ids) { createdIds ->
            Repository.scope.launch {
                val ratings = lastRatingsOf(ids = createdIds)
                createdIds.zipEach(ratings) { id, rating ->
                    states.updateValue(id, rating)
                }
            }
        }
    }

    override fun averageRatingStateOf(ids: List<String>): StateFlow<AverageRating> {
        // could theoretically be optimized by skipping the ordering of the rating list by the order of ids, since that
        // is irrelevant to the average
        return ratingStatesOf(ids = ids).combineState { AverageRating(ids, it) }
    }

    override fun rate(id: String, rating: Rating?) {
        Repository.scope.launch {
            // assumes the new rating is always newer than the most recent one in the DB (unlike old implementation)
            states.updateValue(id, rating)

            val userId = UserRepository.requireCurrentUserId
            if (rating == null) {
                // TODO just add a null rating on top rather than clearing history?
                KotifyDatabase.transaction("clear rating for track id $id") {
                    TrackRatingTable.deleteWhere { (track eq id) and (TrackRatingTable.userId eq userId) }
                }
            } else {
                KotifyDatabase.transaction("add rating for track id $id") {
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

    override suspend fun ratedEntities(userId: String): Set<String> {
        return KotifyDatabase.transaction("load rated track ids for user $userId") {
            TrackRatingTable
                .slice(TrackRatingTable.track)
                .select { TrackRatingTable.userId eq userId }
                .distinct()
                .mapTo(mutableSetOf()) { it[TrackRatingTable.track].value }
        }
    }

    override fun clearAllRatings(userId: String?) {
        Repository.scope.launch {
            KotifyDatabase.transaction("clear ratings for user $userId") {
                if (userId == null) {
                    TrackRatingTable.deleteAll()
                } else {
                    TrackRatingTable.deleteWhere { TrackRatingTable.userId eq userId }
                }
            }

            if (userId == null || userId == UserRepository.requireCurrentUserId) {
                states.computeAll { null } // clear values from StateFlows
                states.clear() // not strictly necessary, but might as well clear the map
            }
        }
    }

    private suspend fun lastRatingOf(id: String, userId: String = UserRepository.requireCurrentUserId): Rating? {
        return KotifyDatabase.transaction("load last rating of track id $id") {
            TrackRatingTable
                .select { TrackRatingTable.track eq id }
                .andWhere { TrackRatingTable.userId eq userId }
                .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.asRating()
        }
    }

    private suspend fun lastRatingsOf(
        ids: List<String>,
        userId: String = UserRepository.requireCurrentUserId,
    ): List<Rating?> {
        // map by ID to ensure returned results are in the same order as the inputs
        val mapById = KotifyDatabase.transaction("load last ratings of ${ids.size} tracks") {
            TrackRatingTable
                .select { TrackRatingTable.track inList ids }
                .andWhere { TrackRatingTable.userId eq userId }
                .groupBy(TrackRatingTable.track)
                .having {
                    TrackRatingTable.rateTime eq Max(TrackRatingTable.rateTime, TrackRatingTable.rateTime.columnType)
                }
                .associate { it[TrackRatingTable.track].value to it.asRating() }
        }

        return ids.map { id -> mapById[id] }
    }

    private fun ResultRow.asRating(): Rating {
        return Rating(
            rating = this[TrackRatingTable.rating],
            maxRating = this[TrackRatingTable.maxRating],
            rateTime = this[TrackRatingTable.rateTime],
        )
    }
}
