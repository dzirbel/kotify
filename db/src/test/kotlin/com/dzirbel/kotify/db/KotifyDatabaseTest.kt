package com.dzirbel.kotify.db

import assertk.assertThat
import com.dzirbel.kotify.util.containsExactlyElementsOfInAnyOrder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(DatabaseExtension::class)
internal class KotifyDatabaseTest {
    private val db = DB.CACHE
    private object TestTable : Table(name = "test_table") {
        val name: Column<String> = text("name")
        val count: Column<Int> = integer("count")
    }

    @BeforeEach
    fun setup() {
        KotifyDatabase.blockingTransaction(db) {
            SchemaUtils.createMissingTablesAndColumns(TestTable)
        }
    }

    @AfterEach
    fun cleanup() {
        KotifyDatabase.blockingTransaction(db) {
            TestTable.deleteAll()
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 5, 20, 20, 20, 20, 20, 50])
    fun testSequentialTransactions(numJobs: Int) {
        runTest {
            repeat(numJobs) { i ->
                KotifyDatabase[db].transaction(name = null) {
                    TestTable.insert { statement ->
                        statement[name] = "row $i"
                        statement[count] = i
                    }
                }
            }

            val rows: List<Pair<String, Int>> = KotifyDatabase[db]
                .transaction(name = null) { TestTable.selectAll().toList() }
                .map { Pair(it[TestTable.name], it[TestTable.count]) }

            assertThat(rows).containsExactlyElementsOfInAnyOrder(
                List(numJobs) { Pair("row $it", it) },
            )
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 5, 20, 20, 20, 20, 20, 50])
    fun testConcurrentTransactions(numJobs: Int) {
        runBlocking(context = Dispatchers.IO) {
            val jobs = List(numJobs) { i ->
                async(start = CoroutineStart.LAZY) {
                    KotifyDatabase[db].transaction(name = null) {
                        TestTable.insert { statement ->
                            statement[name] = "row $i"
                            statement[count] = i
                        }
                    }
                }
            }

            jobs.awaitAll()

            val rows: List<Pair<String, Int>> = KotifyDatabase[db]
                .transaction(name = null) { TestTable.selectAll().toList() }
                .map { Pair(it[TestTable.name], it[TestTable.count]) }

            assertThat(rows).containsExactlyElementsOfInAnyOrder(
                List(numJobs) { Pair("row $it", it) },
            )
        }
    }
}
