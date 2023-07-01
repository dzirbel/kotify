package com.dzirbel.kotify.repository.track

import assertk.Assert
import assertk.assertThat
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class TrackRatingRepositoryTest {
    @AfterEach
    fun cleanup() {
        runBlocking {
            TrackRatingRepository.clearAllRatings(userId = null)
        }
    }

    @Test
    fun testRate() {
        val trackId = "123"
        val rating = Rating(rating = 2)

        runBlocking {
            val rating1 = KotifyDatabase.transaction(name = null) { TrackRatingRepository.lastRatingOf(id = trackId) }
            assertThat(rating1).isNull()

            val ratings1 = KotifyDatabase.transaction(name = null) { TrackRatingRepository.allRatingsOf(id = trackId) }
            assertThat(ratings1).isEmpty()

            val ratingState = TrackRatingRepository.ratingState(id = trackId)
            assertThat(ratingState.value).isNull()

            TrackRatingRepository.rate(id = trackId, rating = rating)

            assertThat(ratingState.value?.rating).isEqualTo(rating.rating)

            KotifyDatabase.transaction(name = null) { TrackRatingRepository.lastRatingOf(id = trackId) }
                .assertThat { isNotNull().matches(rating) }

            KotifyDatabase.transaction(name = null) { TrackRatingRepository.allRatingsOf(id = trackId) }
                .assertThat { isNotNull().matches(listOf(rating)) }
        }
    }

    @Test
    fun testClearRating() {
        val trackId = "123"
        val rating = Rating(rating = 2)

        val state = runBlocking { TrackRatingRepository.ratingState(id = trackId) }

        runBlocking {
            TrackRatingRepository.rate(id = trackId, rating = rating)
        }

        assertThat(state.value).isNotNull().matches(rating)
        runBlocking {
            KotifyDatabase.transaction(name = null) { TrackRatingRepository.lastRatingOf(id = trackId) }
        }
            .assertThat { isNotNull().matches(rating) }

        runBlocking {
            TrackRatingRepository.rate(id = trackId, rating = null)
        }

        assertThat(
            runBlocking {
                KotifyDatabase.transaction(name = null) { TrackRatingRepository.lastRatingOf(id = trackId) }
            },
        )
            .isNull()
        assertThat(state.value).isNull()
    }

    private fun Assert<Rating>.matches(other: Rating) {
        given { actual ->
            assertThat(actual.rating).isEqualTo(other.rating)
            assertThat(actual.maxRating).isEqualTo(other.maxRating)
            assertThat(actual.rateTime.toEpochMilli() - other.rateTime.toEpochMilli()).isEqualTo(0)
        }
    }

    private fun Assert<List<Rating>>.matches(others: List<Rating>) {
        given { actual ->
            assertThat(actual).hasSameSizeAs(others)
            actual.zipEach(others) { e1, e2 -> assertk.assertThat(e1).matches(e2) }
        }
    }

    // TODO unify in utils module
    private fun <T> T.assertThat(assertion: Assert<T>.() -> Unit) {
        assertThat(this).assertion()
    }
}
