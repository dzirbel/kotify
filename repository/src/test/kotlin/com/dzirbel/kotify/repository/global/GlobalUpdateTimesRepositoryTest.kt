package com.dzirbel.kotify.repository.global

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
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
            val updated = KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.updated(key = "dne") }
            assertThat(updated).isNull()

            val hasBeenUpdated = KotifyDatabase.transaction(name = null) {
                GlobalUpdateTimesRepository.hasBeenUpdated(key = "dne")
            }
            assertThat(hasBeenUpdated).isFalse()
        }
    }

    @Test
    fun testSetAndGet() {
        val key = "set-and-get-key"

        runBlocking {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.updated(key = key) })
                .isNotNull()
                .isBetween(start, end)
            assertThat(
                KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) },
            ).isTrue()

            val start2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.updated(key = key) })
                .isNotNull()
                .isBetween(start2, end2)
            assertThat(
                KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) },
            ).isTrue()
        }
    }

    @Test
    fun testInvalidate() {
        val key = "invalidate-key"

        runBlocking {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.setUpdated(key = key) }
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.updated(key = key) })
                .isNotNull()
                .isBetween(start, end)
            assertThat(
                KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) },
            ).isTrue()

            KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.invalidate(key = key) }

            assertThat(KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.updated(key = key) })
                .isNull()
            assertThat(
                KotifyDatabase.transaction(name = null) { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) },
            ).isFalse()
        }
    }
}
