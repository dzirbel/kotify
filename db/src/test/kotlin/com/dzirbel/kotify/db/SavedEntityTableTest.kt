package com.dzirbel.kotify.db

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.util.collections.zipEach
import com.dzirbel.kotify.util.containsExactlyElementsOfInAnyOrder
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

@ExtendWith(DatabaseExtension::class)
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
            assertThat(TestSavedEntityTable.saveState("id", "user")).isNull()
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
            assertThat(TestSavedEntityTable.saveState("id", "user")).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(saved, savedTime.takeIf { saved }))
            assertThat(TestSavedEntityTable.savedEntityIds(userId))
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(entityId) else emptySet())
        }
    }

    @Test
    fun `batch set and get saved state with known savedTime`() {
        val entityIds = List(10) { "id$it" }
        val savedList = entityIds.indices.map { it % 2 == 0 }
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1)
        val savedCheckTime = Instant.ofEpochMilli(2)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityIds = entityIds,
                userId = userId,
                saved = savedList,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            entityIds.zipEach(savedList) { entityId, saved ->
                assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isEqualTo(saved)
                // savedTime is null if not saved
                assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isEqualTo(savedTime.takeIf { saved })
                assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime)
                assertThat(TestSavedEntityTable.saveState(entityId, userId)).isNotNull()
                    .isEqualTo(SavedEntityTable.SaveState(saved, savedTime.takeIf { saved }))
            }

            assertThat(TestSavedEntityTable.savedEntityIds(userId))
                .containsExactlyElementsOfInAnyOrder(entityIds.zip(savedList).filter { it.second }.map { it.first })
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
            assertThat(TestSavedEntityTable.saveState(entityId, userId)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(saved, null))
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
            assertThat(TestSavedEntityTable.saveState(entityId, userId)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(true, savedTime))
            assertThat(TestSavedEntityTable.savedEntityIds(userId)).containsExactlyInAnyOrder(entityId)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `batch update saved state`(knownSaveTime: Boolean) {
        val entityIds = List(20) { "id$it" }
        val initializedEntityIds = entityIds.take(10)
        val savedList = entityIds.indices.map { it % 2 == 0 }
        val savedListInitial = initializedEntityIds.indices.map { it % 3 == 0 }
        val userId = "user"
        val savedTime = Instant.ofEpochMilli(1).takeIf { knownSaveTime }
        val savedCheckTime = Instant.ofEpochMilli(2)

        // initialize saved state for half the entities (with different saved states)
        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityIds = initializedEntityIds,
                userId = userId,
                saved = savedListInitial,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            initializedEntityIds.zipEach(savedListInitial) { entityId, saved ->
                assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isEqualTo(saved)
            }
        }

        // initialize or update all entities
        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityIds = entityIds,
                userId = userId,
                saved = savedList,
                savedTime = savedTime,
                savedCheckTime = savedCheckTime,
            )
        }

        KotifyDatabase.blockingTransaction {
            entityIds.zipEach(savedList) { entityId, saved ->
                assertThat(TestSavedEntityTable.isSaved(entityId, userId)).isNotNull().isEqualTo(saved)
                // savedTime is null if not saved
                assertThat(TestSavedEntityTable.savedTime(entityId, userId)).isEqualTo(savedTime.takeIf { saved })
                assertThat(TestSavedEntityTable.savedCheckTime(entityId, userId)).isNotNull().isEqualTo(savedCheckTime)
                assertThat(TestSavedEntityTable.saveState(entityId, userId)).isNotNull()
                    .isEqualTo(SavedEntityTable.SaveState(saved, savedTime.takeIf { saved }))
            }

            assertThat(TestSavedEntityTable.savedEntityIds(userId))
                .containsExactlyElementsOfInAnyOrder(entityIds.zip(savedList).filter { it.second }.map { it.first })
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
            assertThat(TestSavedEntityTable.saveState(entityId, userId)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(true, null))
            assertThat(TestSavedEntityTable.savedEntityIds(userId)).containsExactlyInAnyOrder(entityId)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `set and get saved state for different users`(saved: Boolean) {
        val entityId = "id"
        val user1 = "user1"
        val user2 = "user2"
        val savedTime1 = Instant.ofEpochMilli(1)
        val savedCheckTime1 = Instant.ofEpochMilli(2)
        val savedTime2 = Instant.ofEpochMilli(3)
        val savedCheckTime2 = Instant.ofEpochMilli(4)

        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = user1,
                saved = saved,
                savedTime = savedTime1,
                savedCheckTime = savedCheckTime1,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, user1)).isNotNull().isEqualTo(saved)
            assertThat(TestSavedEntityTable.savedTime(entityId, user1)).isEqualTo(savedTime1.takeIf { saved })
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, user1)).isNotNull().isEqualTo(savedCheckTime1)
            assertThat(TestSavedEntityTable.saveState(entityId, user1)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(saved, savedTime1.takeIf { saved }))
            assertThat(TestSavedEntityTable.savedEntityIds(user1))
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(entityId) else emptySet())

            assertThat(TestSavedEntityTable.isSaved(entityId, user2)).isNull()
            assertThat(TestSavedEntityTable.savedTime(entityId, user2)).isNull()
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, user2)).isNull()
            assertThat(TestSavedEntityTable.saveState(entityId, user2)).isNull()
            assertThat(TestSavedEntityTable.savedEntityIds(user2)).isEmpty()
        }

        val saved2 = !saved
        KotifyDatabase.blockingTransaction {
            TestSavedEntityTable.setSaved(
                entityId = entityId,
                userId = user2,
                saved = saved2,
                savedTime = savedTime2,
                savedCheckTime = savedCheckTime2,
            )
        }

        KotifyDatabase.blockingTransaction {
            assertThat(TestSavedEntityTable.isSaved(entityId, user1)).isNotNull().isEqualTo(saved)
            assertThat(TestSavedEntityTable.savedTime(entityId, user1)).isEqualTo(savedTime1.takeIf { saved })
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, user1)).isNotNull().isEqualTo(savedCheckTime1)
            assertThat(TestSavedEntityTable.saveState(entityId, user1)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(saved, savedTime1.takeIf { saved }))
            assertThat(TestSavedEntityTable.savedEntityIds(user1))
                .containsExactlyElementsOfInAnyOrder(if (saved) setOf(entityId) else emptySet())

            assertThat(TestSavedEntityTable.isSaved(entityId, user2)).isNotNull().isEqualTo(saved2)
            assertThat(TestSavedEntityTable.savedTime(entityId, user2)).isEqualTo(savedTime2.takeIf { saved2 })
            assertThat(TestSavedEntityTable.savedCheckTime(entityId, user2)).isNotNull().isEqualTo(savedCheckTime2)
            assertThat(TestSavedEntityTable.saveState(entityId, user2)).isNotNull()
                .isEqualTo(SavedEntityTable.SaveState(saved2, savedTime2.takeIf { saved2 }))
            assertThat(TestSavedEntityTable.savedEntityIds(user2))
                .containsExactlyElementsOfInAnyOrder(if (saved2) setOf(entityId) else emptySet())
        }
    }
}
