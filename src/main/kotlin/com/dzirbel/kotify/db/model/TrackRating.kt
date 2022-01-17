package com.dzirbel.kotify.db.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.repository.RatingRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object TrackRatingTable : IntIdTable() {
    val track: Column<EntityID<String>> = reference("track", TrackTable)
    val rating: Column<Int> = integer("rating")
    val maxRating: Column<Int> = integer("max_rating")
    val rateTime: Column<Instant> = timestamp("rate_time")
}

// TODO extract to abstract class if we ever need to re-use logic
object TrackRatingRepository : RatingRepository {
    private val states = ConcurrentHashMap<String, WeakReference<MutableState<Rating?>>>()

    private fun ResultRow.asRating(): Rating {
        return Rating(
            rating = this[TrackRatingTable.rating],
            maxRating = this[TrackRatingTable.maxRating],
            rateTime = this[TrackRatingTable.rateTime],
        )
    }

    override suspend fun lastRatingOf(id: String): Rating? {
        return KotifyDatabase.transaction {
            TrackRatingTable
                .select { TrackRatingTable.track eq id }
                .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.asRating()
        }
    }

    override suspend fun allRatingsOf(id: String): List<Rating> {
        return KotifyDatabase.transaction {
            TrackRatingTable
                .select { TrackRatingTable.track eq id }
                .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
                .map { it.asRating() }
        }
    }

    override suspend fun rate(id: String, rating: Rating?) {
        if (rating == null) {
            KotifyDatabase.transaction {
                TrackRatingTable.deleteWhere { TrackRatingTable.track eq id }
            }

            states[id]?.get()?.value = null
        } else {
            val lastRateTime = KotifyDatabase.transaction {
                val lastRateTime = TrackRatingTable
                    .slice(TrackRatingTable.rateTime)
                    .select { TrackRatingTable.track eq id }
                    .orderBy(TrackRatingTable.rateTime to SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(TrackRatingTable.rateTime)

                TrackRatingTable.insert {
                    it[track] = id
                    it[TrackRatingTable.rating] = rating.rating
                    it[maxRating] = rating.maxRating
                    it[rateTime] = rating.rateTime
                }

                lastRateTime
            }

            if (lastRateTime == null || rating.rateTime >= lastRateTime) {
                states[id]?.get()?.value = rating
            }
        }
    }

    override suspend fun ratedEntities(): Set<String> {
        return KotifyDatabase.transaction {
            TrackRatingTable
                .slice(TrackRatingTable.track)
                .selectAll()
                .distinct()
                .mapTo(mutableSetOf()) { it[TrackRatingTable.track].value }
        }
    }

    override suspend fun ratingState(id: String): State<Rating?> {
        states[id]?.get()?.let { return it }

        val rating = lastRatingOf(id = id)
        val state = mutableStateOf(rating)
        states[id] = WeakReference(state)
        return state
    }

    override suspend fun clearAllRatings() {
        KotifyDatabase.transaction {
            TrackRatingTable.deleteAll()
        }

        for (state in states.values) {
            state.get()?.value = null
        }
    }
}
