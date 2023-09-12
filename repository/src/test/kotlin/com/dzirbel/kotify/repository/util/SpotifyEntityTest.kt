package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.util.CurrentTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

object TestSpotifyEntityTable : SpotifyEntityTable("test") {
    val testColumn: Column<String> = text("test_column")
}

class TestSpotifyEntity(id: EntityID<String>) : SpotifyEntity(id = id, table = TestSpotifyEntityTable) {
    var testColumn: String by TestSpotifyEntityTable.testColumn

    companion object : SpotifyEntityClass<TestSpotifyEntity>(TestSpotifyEntityTable)
}

class TestSpotifyObject(
    override val id: String,
    override val name: String,
    override val type: String = "test",
    override val uri: String? = null,
    override val href: String? = null,
    val testValue: String,
) : SpotifyObject

@ExtendWith(DatabaseExtension::class)
class SpotifyEntityTest {
    private val db = DB.CACHE

    @BeforeEach
    fun setup() {
        KotifyDatabase.blockingTransaction(db) {
            SchemaUtils.createMissingTablesAndColumns(TestSpotifyEntityTable)
        }
    }

    @AfterEach
    fun cleanup() {
        KotifyDatabase.blockingTransaction(db) {
            TestSpotifyEntityTable.deleteAll()
        }
    }

    @Test
    fun updateOrInsert() {
        CurrentTime.mocked {
            val id = "id"
            val networkModel1 = TestSpotifyObject(id = id, name = "test1", testValue = "value1", uri = "uri1")
            val networkModel2 = TestSpotifyObject(id = id, name = "test2", testValue = "value2", uri = "uri2")
            val creationTime = CurrentTime.instant

            val entity1 = KotifyDatabase.blockingTransaction(db) {
                TestSpotifyEntity.updateOrInsert(id, networkModel1, CurrentTime.instant) {
                    testColumn = networkModel1.testValue
                }
            }

            assertThat(entity1.id.value).isEqualTo(id)
            assertThat(entity1.name).isEqualTo(networkModel1.name)
            assertThat(entity1.uri).isEqualTo(networkModel1.uri)
            assertThat(entity1.testColumn).isEqualTo(networkModel1.testValue)
            assertThat(entity1.createdTime).isEqualTo(creationTime)
            assertThat(entity1.updatedTime).isEqualTo(CurrentTime.instant)

            KotifyDatabase.blockingTransaction(db) {
                assertThat(requireNotNull(TestSpotifyEntity.findById(id)).updatedTime).isEqualTo(CurrentTime.instant)
            }

            val count = KotifyDatabase.blockingTransaction(db) { TestSpotifyEntityTable.selectAll().count() }
            assertThat(count).isEqualTo(1)

            CurrentTime.advanceMock(1_000)

            val entity2 = KotifyDatabase.blockingTransaction(db) {
                TestSpotifyEntity.updateOrInsert(id, networkModel2, CurrentTime.instant) {
                    testColumn = networkModel2.testValue
                }
            }

            assertThat(entity2.id.value).isEqualTo(id)
            assertThat(entity2.name).isEqualTo(networkModel2.name)
            assertThat(entity2.uri).isEqualTo(networkModel2.uri)
            assertThat(entity2.testColumn).isEqualTo(networkModel2.testValue)
            assertThat(entity2.createdTime).isEqualTo(creationTime)
            assertThat(entity2.updatedTime).isEqualTo(CurrentTime.instant)

            KotifyDatabase.blockingTransaction(db) {
                assertThat(requireNotNull(TestSpotifyEntity.findById(id)).updatedTime).isEqualTo(CurrentTime.instant)
            }
        }
    }
}
