package com.dzirbel.kotify.repository.rating

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.util.combineState
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

// TODO unit test
open class TrackRatingRepository internal constructor(
    private val applicationScope: CoroutineScope,
    private val userSessionScope: CoroutineScope,
) : RatingRepository {

    private val states = SynchronizedWeakStateFlowMap<String, Rating>()

    override fun ratingStateOf(id: String): StateFlow<Rating?> {
        Repository.checkEnabled()
        return states.getOrCreateStateFlow(key = id) {
            userSessionScope.launch {
                val rating = KotifyDatabase.transaction("load last rating of track id $id") { lastRatingOf(id = id) }
                states.updateValue(id, rating)
            }
        }
    }

    override fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>> {
        Repository.checkEnabled()
        return states.getOrCreateStateFlows(keys = ids) { creations ->
            userSessionScope.launch {
                val createdIds = creations.keys.toList() // convert to list to ensure consistent order
                val ratings = KotifyDatabase.transaction("load last ratings of ${createdIds.size} tracks") {
                    createdIds.map { lastRatingOf(id = it) }
                }
                createdIds.zipEach(ratings) { id, rating ->
                    states.updateValue(id, rating)
                }
            }
        }
    }

    override fun averageRatingStateOf(ids: Iterable<String>): StateFlow<AverageRating> {
        Repository.checkEnabled()
        // could theoretically be optimized by skipping the ordering of the rating list by the order of ids, since that
        // is irrelevant to the average
        return ratingStatesOf(ids = ids).combineState { AverageRating(it.asIterable()) }
    }

    override fun rate(id: String, rating: Rating?) {
        Repository.checkEnabled()
        val userId = UserRepository.requireCurrentUserId
        applicationScope.launch {
            // assumes the new rating is always newer than the most recent one in the DB (unlike old implementation)
            states.updateValue(id, rating)

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
        Repository.checkEnabled()
        return KotifyDatabase.transaction("load rated track ids for user $userId") {
            TrackRatingTable
                .slice(TrackRatingTable.track)
                .select { TrackRatingTable.userId eq userId }
                .distinct()
                .mapTo(mutableSetOf()) { it[TrackRatingTable.track].value }
        }
    }

    override fun clearAllRatings(userId: String?) {
        Repository.checkEnabled()
        applicationScope.launch {
            KotifyDatabase.transaction(userId?.let { "clear ratings for user $userId" } ?: "clear all ratings") {
                if (userId == null) {
                    TrackRatingTable.deleteAll()
                } else {
                    TrackRatingTable.deleteWhere { TrackRatingTable.userId eq userId }
                }
            }

            if (userId == null || userId == UserRepository.requireCurrentUserId) {
                // clear values from StateFlows (necessary even with the clear() to update external references to the
                // StateFlows)
                states.computeAll { null }

                // not strictly necessary, but might as well clear the map
                states.clear()
            }
        }
    }

    private fun lastRatingOf(id: String, userId: String = UserRepository.requireCurrentUserId): Rating? {
        return TrackRatingTable
            .slice(TrackRatingTable.rating, TrackRatingTable.maxRating, TrackRatingTable.rateTime)
            .select { TrackRatingTable.track eq id }
            .andWhere { TrackRatingTable.userId eq userId }
            .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let {
                Rating(
                    rating = it[TrackRatingTable.rating],
                    maxRating = it[TrackRatingTable.maxRating],
                    rateTime = it[TrackRatingTable.rateTime],
                )
            }
    }

    companion object : TrackRatingRepository(
        applicationScope = Repository.applicationScope,
        userSessionScope = Repository.userSessionScope,
    )
}
