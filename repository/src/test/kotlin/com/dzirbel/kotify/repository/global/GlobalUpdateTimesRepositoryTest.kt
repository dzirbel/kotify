package com.dzirbel.kotify.repository.global

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(DatabaseExtension::class)
internal class GlobalUpdateTimesRepositoryTest {
    @Test
    fun testUnset() {
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.updated("dne") }).isNull()
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.hasBeenUpdated("dne") }).isFalse()
    }

    @Test
    fun testSetAndGet() {
        val key = "set-and-get-key"
        val updateTime1 = Instant.ofEpochMilli(50)
        val updateTime2 = Instant.ofEpochMilli(100)

        KotifyDatabase.blockingTransaction {
            GlobalUpdateTimesRepository.setUpdated(key = key, updateTime = updateTime1)
        }

        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.updated(key = key) })
            .isNotNull()
            .isEqualTo(updateTime1)
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
            .isTrue()

        KotifyDatabase.blockingTransaction {
            GlobalUpdateTimesRepository.setUpdated(key = key, updateTime = updateTime2)
        }

        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.updated(key = key) })
            .isNotNull()
            .isEqualTo(updateTime2)
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
            .isTrue()
    }

    @Test
    fun testInvalidate() {
        val key = "invalidate-key"
        val updateTime = Instant.ofEpochMilli(50)

        KotifyDatabase.blockingTransaction {
            GlobalUpdateTimesRepository.setUpdated(key = key, updateTime = updateTime)
        }

        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.updated(key = key) })
            .isNotNull()
            .isEqualTo(updateTime)
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
            .isTrue()

        KotifyDatabase.blockingTransaction {
            GlobalUpdateTimesRepository.invalidate(key = key)
        }

        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.updated(key = key) })
            .isNull()
        assertThat(KotifyDatabase.blockingTransaction { GlobalUpdateTimesRepository.hasBeenUpdated(key = key) })
            .isFalse()
    }
}
