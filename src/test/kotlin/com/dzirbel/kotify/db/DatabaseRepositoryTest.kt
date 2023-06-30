package com.dzirbel.kotify.db

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.util.containsExactlyElementsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Instant

data class TestNetworkModel(
    override val id: String?,
    override val href: String? = "href",
    override val type: String = "test-type",
    override val uri: String? = "uri",
    override val name: String,

    val stringField: String,
    val intField: Int,
    val booleanField: Boolean,
) : SpotifyObject

object TestEntityTable : SpotifyEntityTable() {
    val string: Column<String> = text("string")
    val int: Column<Int> = integer("int")
    val boolean: Column<Boolean> = bool("boolean")
}

class TestEntity(id: EntityID<String>) : SpotifyEntity(id = id, table = TestEntityTable) {
    var string: String by TestEntityTable.string
    var int: Int by TestEntityTable.int
    var boolean: Boolean by TestEntityTable.boolean

    companion object : SpotifyEntityClass<TestEntity, TestNetworkModel>(TestEntityTable) {
        override fun TestEntity.update(networkModel: TestNetworkModel) {
            string = networkModel.stringField
            int = networkModel.intField
            boolean = networkModel.booleanField
        }
    }
}

private object TestRepository : DatabaseRepository<TestEntity, TestNetworkModel>(TestEntity) {
    val fetchedIds = mutableListOf<String>()
    val batchFetchedIds = mutableListOf<String>()

    override suspend fun fetch(id: String): TestNetworkModel? {
        synchronized(fetchedIds) {
            fetchedIds.add(id)
        }

        return remoteModels[id]
    }

    override suspend fun fetch(ids: List<String>): List<TestNetworkModel?> {
        synchronized(batchFetchedIds) {
            batchFetchedIds.addAll(ids)
        }

        return ids.map { remoteModels[it] }
    }
}

private val remoteModels = mapOf(
    "id1" to TestNetworkModel(
        id = "id1",
        name = "Object 1",
        stringField = "str1",
        intField = 2,
        booleanField = true,
    ),
    "id2" to TestNetworkModel(
        id = "id2",
        name = "Object 2",
        stringField = "str2",
        intField = 4,
        booleanField = false,
    ),
    "id3" to TestNetworkModel(
        id = "id3",
        name = "Object 3",
        stringField = "str3",
        intField = 8,
        booleanField = false,
    ),
)

internal class DatabaseRepositoryTest {
    @BeforeEach
    fun setup() {
        transaction(KotifyDatabase.db) {
            SchemaUtils.create(TestEntityTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction(KotifyDatabase.db) { TestEntityTable.deleteAll() }
        TestRepository.fetchedIds.clear()
        TestRepository.batchFetchedIds.clear()
        TestRepository.clearStates()
    }

    @Test
    fun testEmpty() {
        runTest {
            assertThat(TestRepository.getCached(id = "id1")).isNull()

            assertThat(TestRepository.getCached(ids = List(3) { "id$it" })).all {
                hasSize(3)
                each { it.isNull() }
            }
        }
    }

    @Test
    fun testGetRemote() {
        runTest {
            val start = Instant.now()
            val result = TestRepository.getRemote(id = "id1")
            val end = Instant.now()

            requireNotNull(result)
            result.assertMatches(remoteModels.getValue("id1"), createStart = start, createEnd = end)

            assertThat(TestRepository.fetchedIds).containsExactly("id1")
            assertThat(TestRepository.batchFetchedIds).isEmpty()
        }
    }

    @Test
    fun testGetRemoteNull() {
        runTest {
            val result = TestRepository.getRemote(id = "dne")
            assertThat(result).isNull()

            assertThat(TestRepository.fetchedIds).containsExactly("dne")
            assertThat(TestRepository.batchFetchedIds).isEmpty()
        }
    }

    @Test
    fun testGetSequence() {
        val id = "id1"
        runTest {
            // first attempt is not in cache
            val result1 = TestRepository.getCached(id = id)
            assertThat(result1).isNull()
            assertThat(TestRepository.fetchedIds).isEmpty()
            assertThat(TestRepository.batchFetchedIds).isEmpty()

            // second attempt fetches from the remote
            val start2 = Instant.now()
            val result2 = TestRepository.get(id = id)
            val end2 = Instant.now()

            requireNotNull(result2)
            result2.assertMatches(remoteModels.getValue(id), createStart = start2, createEnd = end2)
            assertThat(TestRepository.fetchedIds).containsExactly(id)
            assertThat(TestRepository.batchFetchedIds).isEmpty()

            // third attempt fetches from the cache
            val result3 = TestRepository.get(id = id)

            requireNotNull(result3)
            result3.assertMatches(remoteModels.getValue(id), createStart = start2, createEnd = end2)
            assertThat(TestRepository.fetchedIds).containsExactly(id)
            assertThat(TestRepository.batchFetchedIds).isEmpty()

            // fourth attempt fetches again from the remote, updating the model
            val start4 = Instant.now()
            val result4 = TestRepository.get(id = id, allowCache = false)
            val end4 = Instant.now()

            requireNotNull(result4)
            result4.assertMatches(
                networkModel = remoteModels.getValue(id),
                createStart = start2,
                createEnd = end2,
                updateStart = start4,
                updateEnd = end4,
            )
            assertThat(TestRepository.fetchedIds).containsExactly(id, id)
            assertThat(TestRepository.batchFetchedIds).isEmpty()
        }
    }

    @Test
    fun testGetBatched() {
        runTest {
            val cachedValue = remoteModels.entries.first()
            transaction(KotifyDatabase.db) { TestEntity.from(cachedValue.value) }

            val result = TestRepository.get(ids = remoteModels.keys.toList())
            assertThat(result).hasSize(remoteModels.size)
            remoteModels.forEach { (_, networkModel) ->
                // every result model matches one network model
                val matchingCount = result.count {
                    runCatching { it?.assertMatches(networkModel) }.isSuccess
                }
                assertThat(matchingCount).isEqualTo(1)
            }

            assertThat(TestRepository.fetchedIds).isEmpty()
            assertThat(TestRepository.batchFetchedIds)
                .containsExactlyElementsOf(remoteModels.keys.minus(cachedValue.key).toList())
        }
    }

    @RepeatedTest(100)
    fun testGetParallel() {
        runTest {
            val job1a = async(Dispatchers.IO) { TestRepository.get(id = "id1") }
            val job2a = async(Dispatchers.IO) { TestRepository.get(id = "id2") }

            val result1a = job1a.await()
            val result2a = job2a.await()

            requireNotNull(result1a)
            requireNotNull(result2a)

            result1a.assertMatches(remoteModels.getValue("id1"))
            result2a.assertMatches(remoteModels.getValue("id2"))

            val job1b = async(Dispatchers.IO) { TestRepository.get(id = "id1") }
            val job2b = async(Dispatchers.IO) { TestRepository.get(id = "id2") }

            val result1b = job1b.await()
            val result2b = job2b.await()

            requireNotNull(result1b)
            requireNotNull(result2b)

            result1b.assertMatches(remoteModels.getValue("id1"))
            result2b.assertMatches(remoteModels.getValue("id2"))
        }
    }

    @Test
    fun testStateOf() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                val state = TestRepository.stateOf(id = "id1", scope = this)

                assertThat(TestRepository.fetchedIds).isEmpty()
                assertThat(state.value).isNull()

                advanceUntilIdle()

                assertThat(state.value).isNotNull()
                requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"))
                assertThat(TestRepository.fetchedIds).containsExactly("id1")
            }
        }
    }

    @Test
    fun testStateOfNoRemoteUncached() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                var onStateInitializedValue: TestEntity? = null
                val state = TestRepository.stateOf(
                    id = "id1",
                    scope = this,
                    allowRemote = false,
                    onStateInitialized = { onStateInitializedValue = it },
                )

                assertThat(state.value).isNull()
                assertThat(onStateInitializedValue).isNull()

                advanceUntilIdle()

                assertThat(state.value).isNull()
                assertThat(onStateInitializedValue).isNull()
                assertThat(TestRepository.fetchedIds).isEmpty()

                val start = Instant.now()
                TestRepository.getRemote(id = "id1")
                val end = Instant.now()

                requireNotNull(state.value)
                    .assertMatches(remoteModels.getValue("id1"), createStart = start, createEnd = end)
                assertThat(onStateInitializedValue).isNull()
                assertThat(TestRepository.fetchedIds).containsExactly("id1")

                val stateB = TestRepository.stateOf(id = "id1")
                assertThat(stateB).isSameAs(state)
                assertThat(TestRepository.fetchedIds).containsExactly("id1")
            }
        }
    }

    @Test
    fun testStateOfCached() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                TestRepository.get(id = "id1")
                assertThat(TestRepository.fetchedIds).containsExactly("id1")

                val state = TestRepository.stateOf(id = "id1", scope = this)

                assertThat(state.value).isNull()

                advanceUntilIdle()

                assertThat(state.value).isNotNull()
                requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"))
                assertThat(TestRepository.fetchedIds).containsExactly("id1")
            }
        }
    }

    @Test
    fun testStateOfNoCacheCached() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                TestRepository.get(id = "id1")
                assertThat(TestRepository.fetchedIds).containsExactly("id1")

                val state = TestRepository.stateOf(id = "id1", scope = this, allowCache = false)

                assertThat(state.value).isNull()

                advanceUntilIdle()

                assertThat(state.value).isNotNull()
                requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"))
                assertThat(TestRepository.fetchedIds).containsExactly("id1", "id1")
            }
        }
    }

    /**
     * Ensure that a second call to stateOf() which does more comprehensive loading (from the remote) than a previous
     * one (just from the cache) still ensures that the value is ultimately loaded.
     */
    @Test
    fun testStateOfReload() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                val state = TestRepository.stateOf(id = "id1", scope = this, allowRemote = false)

                advanceUntilIdle()
                assertThat(state.value).isNull()

                val state2 = TestRepository.stateOf(id = "id1", scope = this, allowRemote = true)
                assertThat(state2).isSameAs(state)

                advanceUntilIdle()

                assertThat(state.value).isNotNull()
                requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"))

                requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"))
                assertThat(TestRepository.fetchedIds).containsExactly("id1")
            }
        }
    }

    @Test
    fun testStatesOf() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                TestRepository.get(id = "id2")
                assertThat(TestRepository.fetchedIds).containsExactly("id2")

                val states = TestRepository.statesOf(ids = listOf("id1", "id2", "id3"), scope = this)
                assertThat(states).hasSize(3)
                assertThat(states).extracting { it.value }.each { it.isNull() }

                advanceUntilIdle()

                assertThat(states).index(0).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id1")
                assertThat(states).index(1).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id2")
                assertThat(states).index(2).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id3")

                assertThat(TestRepository.batchFetchedIds).containsExactly("id1", "id3")
                assertThat(TestRepository.fetchedIds).containsExactly("id2")
            }
        }
    }

    @Test
    fun testStatesOfNoRemote() {
        KotifyDatabase.withSynchronousTransactions {
            runTest {
                TestRepository.get(id = "id2")
                assertThat(TestRepository.fetchedIds).containsExactly("id2")

                val states = TestRepository.statesOf(
                    ids = listOf("id1", "id2", "id3"),
                    scope = this,
                    allowRemote = false,
                )
                assertThat(states).hasSize(3)
                assertThat(states).extracting { it.value }.each { it.isNull() }

                advanceUntilIdle()

                assertThat(states).index(0).transform { it.value }.isNull()
                assertThat(states).index(1).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id2")
                assertThat(states).index(2).transform { it.value }.isNull()

                TestRepository.get(id = "id1")
                assertThat(TestRepository.fetchedIds).containsExactly("id2", "id1")

                assertThat(states).index(0).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id1")
                assertThat(states).index(1).transform { it.value }
                    .isNotNull()
                    .transform { it.id.value }
                    .isEqualTo("id2")
                assertThat(states).index(2).transform { it.value }.isNull()
            }
        }
    }

    private fun TestEntity.assertMatches(
        networkModel: TestNetworkModel,
        createStart: Instant? = null,
        createEnd: Instant? = null,
        updateStart: Instant? = createStart,
        updateEnd: Instant? = createEnd,
    ) {
        val id = id.value
        transaction(KotifyDatabase.db) {
            assertThat(id).isEqualTo(networkModel.id)
            assertThat(name).isEqualTo(networkModel.name)

            if (createStart != null && createEnd != null) {
                assertThat(createdTime).isBetween(createStart, createEnd)
            }

            if (updateStart != null && updateEnd != null) {
                assertThat(updatedTime).isBetween(updateStart, updateEnd)
            }

            assertThat(string).isEqualTo(networkModel.stringField)
            assertThat(int).isEqualTo(networkModel.intField)
            assertThat(boolean).isEqualTo(networkModel.booleanField)
        }
    }
}
