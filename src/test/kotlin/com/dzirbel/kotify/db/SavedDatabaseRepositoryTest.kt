package com.dzirbel.kotify.db

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.containsExactlyElementsOfInAnyOrder
import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

data class TestSavedNetworkModel(val id: String)

object TestSavedEntityTable : SavedEntityTable()

object TestSavedRepository : SavedDatabaseRepository<TestSavedNetworkModel>(
    entityName = "test",
    savedEntityTable = TestSavedEntityTable,
) {
    private val fetchedIds: MutableList<List<String>> = mutableListOf()
    private val pushedIds: MutableList<Pair<List<String>, Boolean>> = mutableListOf()
    private var libraryFetches = 0
    private val fromIds: MutableList<String> = mutableListOf()

    val savedOverrides: MutableMap<String, Boolean> = mutableMapOf()

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        fetchedIds.add(ids)
        return ids.map { remoteLibrary.contains(it) || savedOverrides[it] == true }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        pushedIds.add(Pair(ids, saved))
    }

    override suspend fun fetchLibrary(): Iterable<TestSavedNetworkModel> {
        libraryFetches++
        return remoteLibrary.map { TestSavedNetworkModel(id = it) }
    }

    override fun from(savedNetworkType: TestSavedNetworkModel): String {
        fromIds.add(savedNetworkType.id)
        return savedNetworkType.id
    }

    fun assertCalls(
        fetchedIds: List<List<String>> = emptyList(),
        pushedIds: List<Pair<List<String>, Boolean>> = emptyList(),
        libraryFetches: Int = 0,
        fromIds: List<String> = emptyList(),
    ) {
        assertThat(this.fetchedIds).isEqualTo(fetchedIds)
        assertThat(this.pushedIds).isEqualTo(pushedIds)
        assertThat(this.libraryFetches).isEqualTo(libraryFetches)
        assertThat(this.fromIds).isEqualTo(fromIds)
    }

    fun clearCalls() {
        fetchedIds.clear()
        pushedIds.clear()
        libraryFetches = 0
        fromIds.clear()
        savedOverrides.clear()
        clearStates()
    }
}

private val remoteLibrary = listOf("saved-1", "saved-2", "saved-3")

internal class SavedDatabaseRepositoryTest {
    @BeforeEach
    fun setup() {
        transaction(KotifyDatabase.db) {
            SchemaUtils.create(TestSavedEntityTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction(KotifyDatabase.db) {
            TestSavedEntityTable.deleteAll()
            GlobalUpdateTimesTable.deleteAll()
        }

        TestSavedRepository.clearCalls()
    }

    @Test
    fun testEmpty() {
        runTest {
            assertThat(TestSavedRepository.getCached(id = "dne"))
                .isNull()

            assertThat(TestSavedRepository.getCached(ids = listOf("dne1", "dne2")))
                .containsExactly(null, null)

            assertThat(TestSavedRepository.getLibraryCached())
                .isNull()

            assertThat(TestSavedRepository.libraryUpdated())
                .isNull()

            TestSavedRepository.assertCalls()
        }
    }

    @Test
    fun testIsSaved() {
        runTest {
            val saved1 = TestSavedRepository.get(id = "saved-1")
            assertThat(saved1).isNotNull().isTrue()

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1")))

            val saved2 = TestSavedRepository.get(id = "unsaved")
            assertThat(saved2).isNotNull().isFalse()

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1"), listOf("unsaved")))

            val saved3 = TestSavedRepository.getCached(ids = listOf("saved-1", "saved-2", "unsaved"))
            assertThat(saved3).containsExactly(true, null, false)

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1"), listOf("unsaved")))
        }
    }

    @Test
    fun testSetSaved() {
        runTest {
            TestSavedRepository.setSaved(id = "unsaved-1", false)

            TestSavedRepository.assertCalls(pushedIds = listOf(listOf("unsaved-1") to false))

            val saved1 = TestSavedRepository.get(id = "unsaved-1")
            assertThat(saved1).isNotNull().isFalse()
            TestSavedRepository.assertCalls(pushedIds = listOf(listOf("unsaved-1") to false))

            TestSavedRepository.setSaved(ids = listOf("unsaved-2", "saved-2"), true)
            TestSavedRepository.assertCalls(
                pushedIds = listOf(listOf("unsaved-1") to false, listOf("unsaved-2", "saved-2") to true),
            )
            val saved2 = TestSavedRepository.getCached(ids = listOf("unsaved-2", "saved-2"))
            assertThat(saved2).containsExactly(true, true)

            TestSavedRepository.setSaved(id = "saved-2", false)
            TestSavedRepository.assertCalls(
                pushedIds = listOf(
                    listOf("unsaved-1") to false,
                    listOf("unsaved-2", "saved-2") to true,
                    listOf("saved-2") to false,
                ),
            )
            assertThat(TestSavedRepository.getCached(id = "saved-2")).isNotNull().isFalse()
        }
    }

    @Test
    fun testLibrary() {
        runTest {
            val start = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val library = TestSavedRepository.getLibrary()
            val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            assertThat(library).containsExactlyElementsOfInAnyOrder(remoteLibrary)
            assertThat(TestSavedRepository.libraryUpdated()).isNotNull().isBetween(start, end)

            remoteLibrary.forEach { id ->
                assertThat(TestSavedRepository.getCached(id = id)).isNotNull().isTrue()
            }
            assertThat(TestSavedRepository.getCached(id = "unsaved")).isNotNull().isFalse()

            TestSavedRepository.assertCalls(libraryFetches = 1, fromIds = remoteLibrary)

            val cachedLibrary = TestSavedRepository.getLibraryCached()

            assertThat(cachedLibrary).isNotNull().containsExactlyElementsOfInAnyOrder(remoteLibrary)
            assertThat(TestSavedRepository.libraryUpdated()).isNotNull().isBetween(start, end)

            TestSavedRepository.assertCalls(libraryFetches = 1, fromIds = remoteLibrary)

            val cachedLibrary2 = TestSavedRepository.getLibrary()

            assertThat(cachedLibrary2).containsExactlyElementsOfInAnyOrder(remoteLibrary)
            assertThat(TestSavedRepository.libraryUpdated()).isNotNull().isBetween(start, end)

            TestSavedRepository.assertCalls(libraryFetches = 1, fromIds = remoteLibrary)
        }
    }

    @Test
    fun testInvalidateLibrary() {
        runTest {
            val library = TestSavedRepository.getLibraryRemote()
            assertThat(library).containsExactlyElementsOfInAnyOrder(remoteLibrary)

            TestSavedRepository.invalidateLibrary()

            val cachedLibrary = TestSavedRepository.getLibraryCached()
            assertThat(cachedLibrary).isNull()
            assertThat(TestSavedRepository.libraryUpdated()).isNull()

            remoteLibrary.forEach { id ->
                assertThat(TestSavedRepository.getCached(id = id)).isNotNull().isTrue()
            }
            assertThat(TestSavedRepository.getCached(id = "unsaved")).isNull()
        }
    }

    @Test
    fun testLibraryChanged() {
        runTest {
            val library = TestSavedRepository.getLibraryRemote()
            assertThat(library).containsExactlyElementsOfInAnyOrder(remoteLibrary)

            TestSavedRepository.save(id = "saved-x")
            assertThat(TestSavedRepository.getLibraryCached())
                .isNotNull()
                .containsExactlyElementsOfInAnyOrder(remoteLibrary.plus("saved-x"))

            TestSavedRepository.unsave(id = "saved-1")
            assertThat(TestSavedRepository.getLibraryCached())
                .isNotNull()
                .containsExactlyElementsOfInAnyOrder(remoteLibrary.plus("saved-x").minus("saved-1"))

            TestSavedRepository.savedOverrides["saved-y"] = true
            TestSavedRepository.getRemote(id = "saved-y")
            assertThat(TestSavedRepository.getLibraryCached())
                .isNotNull()
                .containsExactlyElementsOfInAnyOrder(remoteLibrary.plus("saved-x").minus("saved-1").plus("saved-y"))
        }
    }

    @Test
    fun testState() {
        runTest {
            val state = TestSavedRepository.stateOf(id = "saved-1", fetchMissing = false)
            assertThat(state.value).isNull()

            TestSavedRepository.setSaved(id = "saved-1", false)
            assertThat(state.value).isNotNull().isFalse()

            TestSavedRepository.savedOverrides["saved-1"] = true
            TestSavedRepository.getRemote(id = "saved-1")
            assertThat(state.value).isNotNull().isTrue()

            val state2 = TestSavedRepository.stateOf(id = "saved-2", fetchMissing = false)
            assertThat(state2.value).isNull()

            TestSavedRepository.getLibrary()
            assertThat(state2.value).isNotNull().isTrue()

            val state3 = TestSavedRepository.stateOf(id = "saved-3", fetchMissing = false)
            assertThat(state3.value).isNotNull().isTrue()
        }
    }

    /**
     * Test that [SavedDatabaseRepository] does not hold strong references to its saved states, so they can be garbage
     * collected when the callers no longer reference them (or in this case, only reference them weakly).
     *
     * This test runs until the reference is cleared, so it will time out if it is never cleared. Since garbage
     * collection is non-deterministic, this is not a perfect solution but the behavior is important to test.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testStateGC() {
        runBlocking {
            val state1 = WeakReference(TestSavedRepository.stateOf(id = "saved-1"))

            var attempt = 1
            while (!state1.refersTo(null)) {
                System.gc()

                // increase delay quadratically over time to allow for slower garbage collection but still run the test
                // quickly if it is faster
                delay(10L * attempt * attempt)
                attempt++
            }

            assertThat(state1.refersTo(null)).isTrue()
        }
    }
}
