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

// TODO test multiple users
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
            assertThat(TestSavedEntityTable.isSaved("id", "user")).isNull()
            assertThat(TestSavedEntityTable.savedTime("id", "user")).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime("id", "user")).isNull()
            assertThat(TestSavedEntityTable.savedEntityIds("user")).isEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `set and get saved state with known savedTime`(saved: Boolean) {
        val entityId = "id"
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime = Instant.ofEpochMilli(2)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = saved,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isEqualTo(saved)
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isEqualTo(savedTime.takeIf { saved })
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime)
            assertThat(TestSavedEntityTable.savedEntityIds(userId))
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(entityId) else emptySet())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `set and get saved state with unknown savedTime`(saved: Boolean) {
        val entityId = "id"
        val userId = "user"
        val savedCheckTime = Instant.ofEpochMilli(1)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = saved,
                savedTime = null,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isEqualTo(saved)
            assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime)
            assertThat(TestSavedEntityTable.savedEntityIds(userId))
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(entityId) else emptySet())
        }
    }

    @Test
    fun `update saved state with known savedTime`() {
        val entityId = "id"
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(2)
        val savedCheckTime2 = Instant.ofEpochMilli(3)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = false,
                savedTime = null,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = true,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isTrue()
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isEqualTo(savedTime)
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime2)
            assertThat(TestSavedEntityTable.savedEntityIds(userId)).containsExactlyInAnyOrder(entityId)
        }
    }

    @Test
    fun `update saved state with unknown savedTime`() {
        val entityId = "id"
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(2)
        val savedCheckTime2 = Instant.ofEpochMilli(3)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = false,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = userId,
                saved = true,
                savedTime = null,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isTrue()
            // savedTime is null if not saved
            assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime2)
            assertThat(TestSavedEntityTable.savedEntityIds(userId)).containsExactlyInAnyOrder(entityId)
        }
    }

    @Test
    fun `batch set and update saved states`() {
        val entityIds = listOf("id1", "id2", "id3")
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(2)
        val savedCheckTime2 = Instant.ofEpochMilli(3)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityIds.first(),
                userId = userId,
                saved = false,
                savedTime = null,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityIds.first(), userId)).isNotNull().isFalse()
        }

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityIds = entityIds,
                userId = userId,
                saved = true,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            for (id in entityIds) {
                assertThat(TestSavedEntityTable.isSaved(id, userId)).isNotNull().isTrue()
                assertThat(TestSavedEntityTable.savedTime(id, userId)).isNotNull().isEqualTo(savedTime)
                assertThat(TestSavedEntityTable.savedCheckTime(id, userId)).isNotNull().isEqualTo(savedCheckTime2)
            }
            assertThat(TestSavedEntityTable.savedEntityIds(userId)).containsExactlyElementsOfInAnyOrder(entityIds)
        }
    }
}
