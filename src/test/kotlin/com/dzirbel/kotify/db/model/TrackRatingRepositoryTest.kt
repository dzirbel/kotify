package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.util.zipEach
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
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
            val rating1 = KotifyDatabase.transaction { TrackRatingRepository.lastRatingOf(id = trackId) }
            assertThat(rating1).isNull()

            val ratings1 = KotifyDatabase.transaction { TrackRatingRepository.allRatingsOf(id = trackId) }
            assertThat(ratings1).isEmpty()

            val ratingState = TrackRatingRepository.ratingState(id = trackId)
            assertThat(ratingState.value).isNull()

            TrackRatingRepository.rate(id = trackId, rating = rating)

            assertThat(ratingState.value?.rating).isEqualTo(rating.rating)

            KotifyDatabase.transaction { TrackRatingRepository.lastRatingOf(id = trackId) }
                .assertMatches(rating)

            KotifyDatabase.transaction { TrackRatingRepository.allRatingsOf(id = trackId) }
                .assertMatches(listOf(rating))
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

        state.value.assertMatches(rating)
        runBlocking {
            KotifyDatabase.transaction { TrackRatingRepository.lastRatingOf(id = trackId) }
        }
            .assertMatches(rating)

        runBlocking {
            TrackRatingRepository.rate(id = trackId, rating = null)
        }

        assertThat(
            runBlocking {
                KotifyDatabase.transaction { TrackRatingRepository.lastRatingOf(id = trackId) }
            }
        )
            .isNull()
        assertThat(state.value).isNull()
    }

    private fun Rating?.assertMatches(other: Rating?) {
        requireNotNull(this)
        requireNotNull(other)

        assertThat(rating).isEqualTo(other.rating)
        assertThat(maxRating).isEqualTo(other.maxRating)
        assertThat(rateTime.toEpochMilli() - other.rateTime.toEpochMilli()).isEqualTo(0)
    }

    private fun List<Rating>.assertMatches(others: List<Rating>) {
        assertThat(size).isEqualTo(others.size)
        zipEach(others) { e1, e2 -> e1.assertMatches(e2) }
    }
}
