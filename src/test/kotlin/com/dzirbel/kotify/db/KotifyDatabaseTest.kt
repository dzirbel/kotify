package com.dzirbel.kotify.db

import assertk.assertThat
import com.dzirbel.kotify.containsExactlyElementsOfInAnyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest

internal class KotifyDatabaseTest {
    private object TestTable : Table(name = "test_table") {
        val name: Column<String> = text("name")
        val count: Column<Int> = integer("count")
    }

    @BeforeEach
    fun setup() {
        transaction(KotifyDatabase.db) {
            SchemaUtils.createMissingTablesAndColumns(TestTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction(KotifyDatabase.db) {
            TestTable.deleteAll()
        }
    }

    @RepeatedTest(10)
    fun testConcurrentTransactions() {
        val numJobs = 24
        runBlocking(context = Dispatchers.IO) {
            val jobs = Array(numJobs) { i ->
                async {
                    KotifyDatabase.transaction(name = null) {
                        TestTable.insert {
                            it[name] = "row $i"
                            it[count] = i
                        }
                    }
                }
            }

            @Suppress("SpreadOperator")
            awaitAll(*jobs)

            val rows: List<Pair<String, Int>> = KotifyDatabase
                .transaction(name = null) { TestTable.selectAll().toList() }
                .map { Pair(it[TestTable.name], it[TestTable.count]) }

            assertThat(rows).containsExactlyElementsOfInAnyOrder(
                List(numJobs) { Pair("row $it", it) },
            )
        }
    }
}
