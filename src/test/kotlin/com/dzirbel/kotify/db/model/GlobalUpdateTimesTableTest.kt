package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(DatabaseExtension::class)
internal class GlobalUpdateTimesTableTest {
    @Test
    fun testUnset() {
        runBlocking {
            val updated = KotifyDatabase.transaction { GlobalUpdateTimesTable.updated(key = "dne") }
            assertThat(updated).isNull()
        }
    }

    @Test
    fun testSetAndGet() {
        val key = "set-and-get-key"

        runBlocking {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            transaction { GlobalUpdateTimesTable.setUpdated(key = key) }
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesTable.updated(key = key) })
                .isIn(Range.closed(start, end))

            val start2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            transaction { GlobalUpdateTimesTable.setUpdated(key = key) }
            val end2 = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(KotifyDatabase.transaction { GlobalUpdateTimesTable.updated(key = key) })
                .isIn(Range.closed(start2, end2))
        }
    }
}
