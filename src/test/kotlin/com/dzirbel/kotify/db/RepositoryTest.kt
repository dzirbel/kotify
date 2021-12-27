package com.dzirbel.kotify.db

import com.dzirbel.kotify.network.model.SpotifyObject
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runBlockingTest
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
        return super.fetch(ids)
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
)

// TODO finish testing
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
        assertThat(TestRepository.getCached(id = "id1")).isNull()
        assertThat(TestRepository.getCached(ids = List(3) { "id$it" })).isEqualTo(List(3) { null })
    }

    @Test
    fun testGetRemote() {
        runBlockingTest {
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
