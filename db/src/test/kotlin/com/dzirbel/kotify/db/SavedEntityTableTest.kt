package com.dzirbel.kotify.db

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.util.containsExactlyElementsOfInAnyOrder
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

internal class SavedEntityTableTest {
    private object TestSavedEntityTable : SavedEntityTable(name = "test_saved_entities")

    @BeforeEach
    fun setup() {
        KotifyDatabase.blockingTransaction { SchemaUtils.create(TestSavedEntityTable) }
    }

    @AfterEach
    fun cleanup() {
        KotifyDatabase.blockingTransaction { SchemaUtils.drop(TestSavedEntityTable) }
    }

    @Test
    fun `empty table`() {
        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved("id")).isNull()
            assertThat(TestSavedEntityTable.savedTime("id")).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime("id")).isNull()
            assertThat(TestSavedEntityTable.savedEntityIds()).isEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `set and get saved state with known savedTime`(saved: Boolean) {
        val id = "id"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime = Instant.ofEpochMilli(2)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = saved,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isEqualTo(saved)
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(id)).isEqualTo(savedTime.takeIf { saved })
            assertThat(TestSavedEntityTable.savedCheckTime(id)).isNotNull().isEqualTo(savedCheckTime)
            assertThat(TestSavedEntityTable.savedEntityIds())
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(id) else emptySet())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `set and get saved state with unknown savedTime`(saved: Boolean) {
        val id = "id"
        val savedCheckTime = Instant.ofEpochMilli(1)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = saved,
                savedTime = null,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isEqualTo(saved)
            assertThat(TestSavedEntityTable.savedTime(id)).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime(id)).isNotNull().isEqualTo(savedCheckTime)
            assertThat(TestSavedEntityTable.savedEntityIds())
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(id) else emptySet())
        }
    }

    @Test
    fun `update saved state with known savedTime`() {
        val id = "id"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(2)
        val savedCheckTime2 = Instant.ofEpochMilli(3)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = false,
                savedTime = null,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = true,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isTrue()
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(id)).isEqualTo(savedTime)
            assertThat(TestSavedEntityTable.savedCheckTime(id)).isNotNull().isEqualTo(savedCheckTime2)
            assertThat(TestSavedEntityTable.savedEntityIds()).containsExactlyInAnyOrder(id)
        }
    }

    @Test
    fun `update saved state with unknown savedTime`() {
        val id = "id"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(1)
        val savedCheckTime2 = Instant.ofEpochMilli(2)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = false,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = id,
                saved = true,
                savedTime = null,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isTrue()
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(id)).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime(id)).isNotNull().isEqualTo(savedCheckTime2)
            assertThat(TestSavedEntityTable.savedEntityIds()).containsExactlyInAnyOrder(id)
        }
    }

    @Test
    fun `batch set and update saved states`() {
        val ids = listOf("id1", "id2", "id3")
        val savedCheckTime1 = Instant.ofEpochMilli(1)
        val savedCheckTime2 = Instant.ofEpochMilli(2)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = ids.first(),
                saved = false,
                savedTime = null,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(ids.first())).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(entityIds = ids, saved = true, savedCheckTime = savedCheckTime2)
        }

        KotifyDatabase.blockingTransaction {
            for (id in ids) {
                assertThat(TestSavedEntityTable.isSaved(id)).isNotNull().isTrue()
                assertThat(TestSavedEntityTable.savedTime(id)).isNull()
                assertThat(TestSavedEntityTable.savedCheckTime(id)).isNotNull().isEqualTo(savedCheckTime2)
            }
            assertThat(TestSavedEntityTable.savedEntityIds()).containsExactlyElementsOfInAnyOrder(ids)
        }
    }
}
