package com.dzirbel.kotify.repository.track

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.repository.RatingRepository
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object TrackRatingRepository : RatingRepository {
    // userId -> [trackId -> reference to state of the rating]
    private val states =
        ConcurrentHashMap<String, ConcurrentHashMap<String, WeakReference<MutableStateFlow<Rating?>>>>()

    private fun ResultRow.asRating(): Rating {
        return Rating(
            rating = this[TrackRatingTable.rating],
            maxRating = this[TrackRatingTable.maxRating],
            rateTime = this[TrackRatingTable.rateTime],
        )
    }

    override suspend fun lastRatingOf(id: String, userId: String): Rating? {
        return TrackRatingTable
            .select { TrackRatingTable.track eq id }
            .andWhere { TrackRatingTable.userId eq userId }
            .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.asRating()
    }

    override suspend fun lastRatingsOf(ids: List<String>, userId: String): List<Rating?> {
        // map by ID to ensure returned results are in the same order as the inputs
        val mapById: Map<String, ResultRow> = TrackRatingTable
            .select { TrackRatingTable.track inList ids }
            .andWhere { TrackRatingTable.userId eq userId }
            .groupBy(TrackRatingTable.track)
            .having {
                TrackRatingTable.rateTime eq Max(TrackRatingTable.rateTime, TrackRatingTable.rateTime.columnType)
            }
            .associateBy { it[TrackRatingTable.track].value }

        return ids.map { id -> mapById[id]?.asRating() }
    }

    override suspend fun allRatingsOf(id: String, userId: String): List<Rating> {
        return TrackRatingTable
            .select { TrackRatingTable.track eq id }
            .andWhere { TrackRatingTable.userId eq userId }
            .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
            .map { it.asRating() }
    }

    override suspend fun rate(id: String, rating: Rating?, userId: String) {
        if (rating == null) {
            KotifyDatabase.transaction("clear rating for track id $id") {
                TrackRatingTable.deleteWhere { track eq id and (TrackRatingTable.userId eq userId) }
            }

            states[userId]?.get(id)?.get()?.value = null
        } else {
            val lastRateTime = KotifyDatabase.transaction("add rating for track id $id") {
                val lastRateTime = TrackRatingTable
                    .slice(TrackRatingTable.rateTime)
                    .select { TrackRatingTable.track eq id }
                    .andWhere { TrackRatingTable.userId eq userId }
                    .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(TrackRatingTable.rateTime)

                TrackRatingTable.insert { statement ->
                    statement[track] = id
                    statement[TrackRatingTable.rating] = rating.rating
                    statement[maxRating] = rating.maxRating
                    statement[rateTime] = rating.rateTime
                    statement[TrackRatingTable.userId] = userId
                }

                lastRateTime
            }

            if (lastRateTime == null || rating.rateTime >= lastRateTime) {
                states[userId]?.get(id)?.get()?.value = rating
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

    override fun ratingState(id: String, userId: String): StateFlow<Rating?> {
        states[userId]?.get(id)?.get()?.let { return it }

        val state = MutableStateFlow<Rating?>(null)

        val userStates = states.getOrPut(userId) { ConcurrentHashMap() }
        userStates[id] = WeakReference(state)

        GlobalScope.launch {
            val rating = KotifyDatabase.transaction("load last rating of track id $id for state") {
                lastRatingOf(id = id, userId = userId)
            }

            state.value = rating
        }

        return state
    }

    override suspend fun ratingStates(ids: List<String>, userId: String): List<StateFlow<Rating?>> {
        val missingIndices = ArrayList<IndexedValue<String>>()

        val existingStates = ids.mapIndexedTo(ArrayList(ids.size)) { index, id ->
            val state = states[userId]?.get(id)?.get()
            if (state == null) {
                missingIndices.add(IndexedValue(index = index, value = id))
            }

            state
        }

        if (missingIndices.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return existingStates as List<StateFlow<Rating?>>
        }

        val missingRatings = KotifyDatabase.transaction("load last ratings of ${ids.size} tracks for states") {
            lastRatingsOf(ids = missingIndices.map { it.value }, userId = userId)
        }

        val userStates = states.getOrPut(userId) { ConcurrentHashMap() }
        missingIndices.zipEach(missingRatings) { indexedValue, rating ->
            val state = MutableStateFlow(rating)
            userStates[indexedValue.value] = WeakReference(state)
            existingStates[indexedValue.index] = state
        }

        @Suppress("UNCHECKED_CAST")
        return existingStates as List<StateFlow<Rating?>>
    }

    override suspend fun clearAllRatings(userId: String?) {
        KotifyDatabase.transaction("clear ratings for user $userId") {
            if (userId == null) {
                TrackRatingTable.deleteAll()
            } else {
                TrackRatingTable.deleteWhere { TrackRatingTable.userId eq userId }
            }
        }

        if (userId == null) {
            for (userStates in states.values) {
                for (state in userStates.values) {
                    state.get()?.value = null
                }
            }
        } else {
            for (state in states[userId]?.values.orEmpty()) {
                state.get()?.value = null
            }
        }
    }
}
