package com.dzirbel.kotify.db

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

data class TestSavedNetworkModel(val id: String)

object TestSavedEntityTable : SavedEntityTable()

object TestSavedRepository : SavedRepository<TestSavedNetworkModel>(savedEntityTable = TestSavedEntityTable) {
    private val fetchedIds: MutableList<List<String>> = mutableListOf()
    private val pushedIds: MutableList<Pair<List<String>, Boolean>> = mutableListOf()
    private var libraryFetches = 0
    private val fromIds: MutableList<String> = mutableListOf()

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        fetchedIds.add(ids)
        return ids.map { remoteLibrary.contains(it) }
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
    }
}

private val remoteLibrary = setOf("saved-1", "saved-2", "saved-3")

// TODO test library calls
// TODO test listeners
// TODO test state
internal class SavedRepositoryTest {
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
        }

        TestSavedRepository.clearCalls()
    }

    @Test
    fun testEmpty() {
        runTest {
            assertThat(TestSavedRepository.isSavedCached(id = "dne"))
                .isNull()

            assertThat(TestSavedRepository.isSavedCached(ids = listOf("dne1", "dne2")))
                .containsExactly(null, null)

            assertThat(TestSavedRepository.getLibraryCached())
                .isNull()

            TestSavedRepository.assertCalls()
        }
    }

    @Test
    fun testIsSaved() {
        runTest {
            val saved1 = TestSavedRepository.isSaved(id = "saved-1")
            assertThat(saved1).isTrue()

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1")))

            val saved2 = TestSavedRepository.isSaved(id = "unsaved")
            assertThat(saved2).isFalse()

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1"), listOf("unsaved")))

            val saved3 = TestSavedRepository.isSavedCached(ids = listOf("saved-1", "saved-2", "unsaved"))
            assertThat(saved3).containsExactly(true, null, false).inOrder()

            TestSavedRepository.assertCalls(fetchedIds = listOf(listOf("saved-1"), listOf("unsaved")))
        }
    }

    @Test
    fun testSetSaved() {
        runTest {
            TestSavedRepository.setSaved(id = "unsaved-1", false)

            TestSavedRepository.assertCalls(pushedIds = listOf(listOf("unsaved-1") to false))

            val saved1 = TestSavedRepository.isSaved(id = "unsaved-1")
            assertThat(saved1).isFalse()
            TestSavedRepository.assertCalls(pushedIds = listOf(listOf("unsaved-1") to false))

            TestSavedRepository.setSaved(ids = listOf("unsaved-2", "saved-2"), true)
            TestSavedRepository.assertCalls(
                pushedIds = listOf(listOf("unsaved-1") to false, listOf("unsaved-2", "saved-2") to true),
            )
        }
    }
}
