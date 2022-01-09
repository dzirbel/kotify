package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(DatabaseExtension::class)
internal class GlobalUpdateTimesRepositoryTest {
    @Test
    fun testUnset() {
        runBlocking {
            val updated = KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(key = "dne") }
            assertThat(updated).isNull()

            val hasBeenUpdated = KotifyDatabase.transaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = "dne") }
            assertThat(hasBeenUpdated).isFalse()
        }
    }

    @Test
    fun testSetAndGet() {
        val key = "set-and-get-key"

        runBlocking {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(key = key) })
                .isIn(Range.closed(start, end))
            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
                .isTrue()

            val start2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(key = key) })
                .isIn(Range.closed(start2, end2))
            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
                .isTrue()
        }
    }

    @Test
    fun testInvalidate() {
        val key = "invalidate-key"

        runBlocking {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(key = key) })
                .isIn(Range.closed(start, end))
            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
                .isTrue()

            KotifyDatabase.transaction { GlobalUpdateTimesRepository.invalidate(key = key) }

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(key = key) })
                .isNull()
            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
                .isFalse()
        }
    }
}
