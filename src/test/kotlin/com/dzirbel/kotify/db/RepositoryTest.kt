package com.dzirbel.kotify.db

import com.dzirbel.kotify.network.model.SpotifyObject
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
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

private object TestRepository : Repository<TestEntity, TestNetworkModel>(TestEntity) {
    val fetchedIds: MutableList<String> = mutableListOf()
    val batchFetchedIds: MutableList<List<String>> = mutableListOf()

    override suspend fun fetch(id: String): TestNetworkModel? {
        fetchedIds.add(id)
        return remoteModels[id]
    }

    override suspend fun fetch(ids: List<String>): List<TestNetworkModel?> {
        batchFetchedIds.add(ids)
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

internal class RepositoryTest {
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
        runBlockingTest {
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
                remoteModels.getValue(id),
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
            transaction(KotifyDatabase.db) { TestRepository.put(cachedValue.value) }

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
                .containsExactly(remoteModels.keys.minus(cachedValue.key).toList())
        }
    }

    @Test
    fun testInvalidate() {
        val id = "id1"
        runBlocking {
            assertThat(TestRepository.getCached(id = id)).isNull()

            transaction(KotifyDatabase.db) { TestRepository.put(requireNotNull(remoteModels[id])) }

            assertThat(TestRepository.getCached(id = id)).isNotNull()

            TestRepository.invalidate(id = id)

            assertThat(TestRepository.getCached(id = id)).isNull()
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
                assertThat(createdTime).isIn(Range.closed(createStart, createEnd))
            }

            if (updateStart != null && updateEnd != null) {
                assertThat(updatedTime).isIn(Range.closed(updateStart, updateEnd))
            }

            assertThat(string).isEqualTo(networkModel.stringField)
            assertThat(int).isEqualTo(networkModel.intField)
            assertThat(boolean).isEqualTo(networkModel.booleanField)
        }
    }
}
