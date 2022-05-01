package com.dzirbel.kotify.db

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.dzirbel.kotify.containsExactlyElementsOf
import com.dzirbel.kotify.isSameInstanceAs
import com.dzirbel.kotify.network.model.SpotifyObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
    }

    @Test
    fun testEmpty() {
        runBlocking {
            assertThat(TestRepository.getCached(id = "id1")).isNull()
            assertThat(TestRepository.getCached(ids = List(3) { "id$it" })).isEqualTo(List(3) { null })
        }
    }

    @Test
    fun testGetRemote() {
        runBlocking {
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
        runBlocking {
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
            val result4 = TestRepository.getRemote(id = id)
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
        runBlocking {
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
        runBlocking {
            val job1a = async(Dispatchers.IO) { TestRepository.get(id = "id1") }
            val job2a = async(Dispatchers.IO) { TestRepository.get(id = "id2") }

            job1a.await()
            job2a.await()

            val job1b = async(Dispatchers.IO) { TestRepository.get(id = "id1") }
            val job2b = async(Dispatchers.IO) { TestRepository.get(id = "id2") }

            job1b.await()
            job2b.await()
        }
    }

    @Test
    fun testStateOf() {
        val state = runBlocking { TestRepository.stateOf(id = "id1", fetchMissing = false) }

        assertThat(state.value).isNull()

        val start = Instant.now()
        runBlocking { TestRepository.get(id = "id1") }
        val end = Instant.now()

        requireNotNull(state.value).assertMatches(remoteModels.getValue("id1"), createStart = start, createEnd = end)

        val stateB = runBlocking { TestRepository.stateOf(id = "id1") }
        assertThat(stateB).isSameInstanceAs(state)

        TestRepository.clearStates()
    }

    @Test
    fun testStatesOf() {
        runBlocking { TestRepository.get(id = "id2") }

        val states = runBlocking {
            TestRepository.stateOf(ids = listOf("id1", "id2", "id3"), fetchMissing = false)
        }

        println(states.map { it.value })
        assertThat(states).hasSize(3)
        assertThat(states).index(0).transform { it.value }.isNull()
        assertThat(states).index(1).transform { it.value }.isNotNull().transform { it.id.value }.isEqualTo("id2")
        assertThat(states).index(2).transform { it.value }.isNull()

        runBlocking { TestRepository.get(id = "id1") }

        assertThat(states).index(0).transform { it.value }.isNotNull().transform { it.id.value }.isEqualTo("id1")
        assertThat(states).index(1).transform { it.value }.isNotNull().transform { it.id.value }.isEqualTo("id2")
        assertThat(states).index(2).transform { it.value }.isNull()

        TestRepository.clearStates()
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
