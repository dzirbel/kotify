package com.dzirbel.kotify.repository

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.containsExactlyElementsOf
import com.dzirbel.kotify.util.elementsSatisfy
import com.dzirbel.kotify.util.isNullIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private class TestRepository<T>(
    private val databaseValues: Map<String, Pair<T, Instant>>,
    private val remoteValues: Map<String, T>,
    private val remoteDelay: Duration? = null,
    scope: CoroutineScope,
) : DatabaseRepository<T, T, T>(entityName = "test", scope = scope) {

    val databaseFetches = mutableListOf<String>()
    val remoteFetches = mutableListOf<String>()

    override fun fetchFromDatabase(id: String): Pair<T, Instant>? {
        databaseFetches.add(id)
        return databaseValues[id]
    }

    override suspend fun fetchFromRemote(id: String): T? {
        if (remoteDelay != null) delay(remoteDelay)
        remoteFetches.add(id)
        return remoteValues[id]
    }

    override fun convertToVM(databaseModel: T) = databaseModel

    override fun convertToDB(id: String, networkModel: T, fetchTime: Instant) = networkModel
}

@ExtendWith(DatabaseExtension::class)
class DatabaseRepositoryTest {
    @Test
    fun loadCachedValue() {
        runTest {
            CurrentTime.mocked {
                KotifyDatabase.withSynchronousTransactions {
                    val repository = TestRepository(
                        databaseValues = mapOf("id" to ("value" to CurrentTime.instant)),
                        remoteValues = emptyMap(),
                        scope = this,
                    )

                    val stateFlow = repository.stateOf("id")

                    assertThat(stateFlow.value).isNull()

                    runCurrent()

                    assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isEqualTo("value")
                    assertThat(repository.databaseFetches).containsExactly("id")
                    assertThat(repository.remoteFetches).isEmpty()
                    assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.DATABASE) })

                    assertThat(repository.stateOf("id")).isSameAs(stateFlow)
                    assertThat(repository.databaseFetches).containsExactly("id")
                    assertThat(repository.remoteFetches).isEmpty()
                    assertThat(repository.log.events).elementsSatisfy(
                        { it.isSuccess(DataSource.DATABASE) },
                        { it.isSuccess(DataSource.MEMORY) },
                    )
                }
            }
        }
    }

    @Test
    fun loadRemoteValue() {
        runTest {
            CurrentTime.mocked {
                KotifyDatabase.withSynchronousTransactions {
                    val repository = TestRepository(
                        databaseValues = emptyMap(),
                        remoteValues = mapOf("id" to "value"),
                        remoteDelay = 1000.milliseconds,
                        scope = this,
                    )

                    val stateFlow = repository.stateOf("id")

                    assertThat(stateFlow.value).isNull()

                    runCurrent()

                    assertThat(stateFlow.value).isEqualTo(CacheState.Refreshing())
                    assertThat(repository.log.events).isEmpty()

                    advanceUntilIdle()

                    assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isEqualTo("value")
                    assertThat(repository.databaseFetches).containsExactly("id")
                    assertThat(repository.remoteFetches).containsExactly("id")
                    assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.REMOTE) })

                    assertThat(repository.stateOf("id")).isSameAs(stateFlow)
                    assertThat(repository.databaseFetches).containsExactly("id")
                    assertThat(repository.remoteFetches).containsExactly("id")
                    assertThat(repository.log.events).elementsSatisfy(
                        { it.isSuccess(DataSource.REMOTE) },
                        { it.isSuccess(DataSource.MEMORY) },
                    )
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    fun loadWithTTL(case: TTLCase) {
        data class DataWithFetchTime(val value: String, val cacheTime: Instant)

        runTest {
            CurrentTime.mocked {
                KotifyDatabase.withSynchronousTransactions {
                    val cacheInstant = Instant.ofEpochMilli(case.cacheTime)
                    val cached = DataWithFetchTime("cached", cacheInstant)
                    val remote = DataWithFetchTime("remote", CurrentTime.instant)
                    val cacheStrategy = CacheStrategy.TTL<DataWithFetchTime>(
                        transientTTL = case.transientTTL,
                        invalidTTL = case.invalidTTL,
                        getUpdateTime = { it.cacheTime },
                    )

                    val repository = TestRepository(
                        databaseValues = mapOf("id" to (cached to cacheInstant)),
                        remoteValues = mapOf("id" to remote),
                        remoteDelay = 1000.milliseconds,
                        scope = this,
                    )

                    val stateFlow = repository.stateOf("id", cacheStrategy = cacheStrategy)

                    assertThat(stateFlow.value).isNull()

                    runCurrent() // load from database, then delay

                    assertThat(repository.databaseFetches).containsExactly("id")
                    assertThat(repository.remoteFetches).isEmpty()
                    when (case.expectedValidity) {
                        CacheStrategy.CacheValidity.VALID -> {
                            assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isSameAs(cached)
                            assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.DATABASE) })
                        }

                        CacheStrategy.CacheValidity.INVALID -> {
                            assertThat(stateFlow.value).isEqualTo(CacheState.Refreshing())
                            assertThat(repository.log.events).isEmpty()
                        }

                        CacheStrategy.CacheValidity.TRANSIENT -> {
                            assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isSameAs(cached)
                            assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.DATABASE) })
                        }
                    }

                    advanceUntilIdle() // finish load from remote

                    assertThat(repository.databaseFetches).containsExactly("id")
                    when (case.expectedValidity) {
                        CacheStrategy.CacheValidity.VALID -> {
                            assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isSameAs(cached)
                            assertThat(repository.remoteFetches).isEmpty()
                            assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.DATABASE) })
                        }

                        CacheStrategy.CacheValidity.INVALID -> {
                            assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isSameAs(remote)
                            assertThat(repository.remoteFetches).containsExactly("id")
                            assertThat(repository.log.events).elementsSatisfy({ it.isSuccess(DataSource.REMOTE) })
                        }

                        CacheStrategy.CacheValidity.TRANSIENT -> {
                            assertThat(stateFlow.value).isNotNull().transform { it.cachedValue }.isSameAs(remote)
                            assertThat(repository.remoteFetches).containsExactly("id")
                            assertThat(repository.log.events).elementsSatisfy(
                                { it.isSuccess(DataSource.DATABASE) },
                                { it.isSuccess(DataSource.REMOTE) },
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun loadWithTTLBatched() {
        data class DataWithFetchTime(val value: String, val cacheTime: Instant)
        val mockedTime = Instant.ofEpochMilli(10_000L)

        runTest {
            CurrentTime.mocked(mockedTime.toEpochMilli()) {
                KotifyDatabase.withSynchronousTransactions {
                    val cacheStrategy = CacheStrategy.TTL<DataWithFetchTime>(
                        transientTTL = 1000.milliseconds,
                        invalidTTL = 2000.milliseconds,
                        getUpdateTime = { it.cacheTime },
                    )

                    val ids = listOf("id1", "id2", "id3", "id4", "id5")
                    val repository = TestRepository(
                        databaseValues = mapOf(
                            "id1" to mockedTime.minusMillis(400).let { DataWithFetchTime("cached1", it) to it },
                            "id2" to mockedTime.minusMillis(600).let { DataWithFetchTime("cached2", it) to it },
                            "id3" to mockedTime.minusMillis(1400).let { DataWithFetchTime("cached3", it) to it },
                            "id4" to mockedTime.minusMillis(1600).let { DataWithFetchTime("cached4", it) to it },
                            "id5" to mockedTime.minusMillis(2500).let { DataWithFetchTime("cached5", it) to it },
                        ),
                        remoteValues = mapOf(
                            "id1" to DataWithFetchTime("remote1", mockedTime),
                            "id2" to DataWithFetchTime("remote2", mockedTime),
                            "id3" to DataWithFetchTime("remote3", mockedTime),
                            "id4" to DataWithFetchTime("remote4", mockedTime),
                            "id5" to DataWithFetchTime("remote5", mockedTime),
                        ),
                        remoteDelay = 50.milliseconds,
                        scope = this,
                    )

                    val stateFlows = repository.statesOf(ids, cacheStrategy = cacheStrategy)

                    assertThat(stateFlows).all {
                        hasSize(ids.size)
                        repeat(ids.size) { i ->
                            index(i).transform { it.value }.isNull()
                        }
                    }

                    runCurrent() // load from database, then delay

                    assertThat(repository.databaseFetches).containsExactlyElementsOf(ids)
                    assertThat(repository.remoteFetches).isEmpty()

                    assertThat(stateFlows[0].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached1")
                    assertThat(stateFlows[1].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached2")
                    assertThat(stateFlows[2].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached3")
                    assertThat(stateFlows[3].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached4")
                    assertThat(stateFlows[4].value).isEqualTo(CacheState.Refreshing())

                    assertThat(repository.log.events).elementsSatisfy(
                        { assert ->
                            assert.transform { it.title }.isEqualTo("loaded 4/5 tests from database")
                            assert.transform { it.data.source }.isEqualTo(DataSource.DATABASE)
                            assert.transform { it.data.timeInDb }.isNotNull()
                            assert.transform { it.data.timeInRemote }.isNull()
                            assert.transform { it.type }.isEqualTo(Log.Event.Type.SUCCESS)
                        },
                        { assert ->
                            assert.transform { it.title }.isEqualTo("missing 3/5 tests in database")
                            assert.transform { it.data.source }.isEqualTo(DataSource.DATABASE)
                            assert.transform { it.data.timeInDb }.isNotNull()
                            assert.transform { it.data.timeInRemote }.isNull()
                            assert.transform { it.type }.isEqualTo(Log.Event.Type.INFO)
                        },
                    )

                    advanceUntilIdle() // finish load from remote

                    assertThat(repository.databaseFetches).containsExactlyElementsOf(ids)
                    assertThat(repository.remoteFetches).containsExactly("id3", "id4", "id5")
                    assertThat(stateFlows[0].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached1")
                    assertThat(stateFlows[1].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("cached2")
                    assertThat(stateFlows[2].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("remote3")
                    assertThat(stateFlows[3].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("remote4")
                    assertThat(stateFlows[4].value).isNotNull().transform { it.cachedValue?.value }.isEqualTo("remote5")

                    assertThat(repository.log.events).elementsSatisfy(
                        { /* no-op: already verified */ },
                        { /* no-op: already verified */ },
                        { assert ->
                            assert.transform { it.title }.isEqualTo("loaded 3/3 tests from remote")
                            assert.transform { it.data.source }.isEqualTo(DataSource.REMOTE)
                            assert.transform { it.data.timeInDb }.isNotNull()
                            assert.transform { it.data.timeInRemote }.isNotNull()
                            assert.transform { it.type }.isEqualTo(Log.Event.Type.SUCCESS)
                        },
                    )
                }
            }
        }
    }

    private fun Assert<Log.Event<Repository.LogData>>.isSuccess(source: DataSource, id: String = "id") {
        all {
            val title = when (source) {
                DataSource.MEMORY -> "state for test $id in memory"
                DataSource.DATABASE -> "loaded test $id from database"
                DataSource.REMOTE -> "loaded test $id from remote"
            }

            transform { it.title }.isEqualTo(title)
            transform { it.data.source }.isEqualTo(source)
            transform { it.data.timeInDb }.isNullIf(source == DataSource.MEMORY)
            transform { it.data.timeInRemote }.isNullIf(source != DataSource.REMOTE)
            transform { it.type }
                .isEqualTo(if (source == DataSource.MEMORY) Log.Event.Type.INFO else Log.Event.Type.SUCCESS)
        }
    }

    data class TTLCase(
        val cacheTime: Long,
        val currentTime: Long,
        val transientTTL: Duration?,
        val invalidTTL: Duration?,
        val expectedValidity: CacheStrategy.CacheValidity,
    )

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            Repository.enabled = true
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            Repository.enabled = false
        }

        @JvmStatic
        fun loadWithTTL(): List<TTLCase> {
            return listOf(
                TTLCase(
                    cacheTime = 1_000,
                    currentTime = 2_000,
                    transientTTL = null,
                    invalidTTL = null,
                    expectedValidity = CacheStrategy.CacheValidity.VALID,
                ),
                TTLCase(
                    cacheTime = 1_000,
                    currentTime = 2_000,
                    transientTTL = 2000.milliseconds,
                    invalidTTL = null,
                    expectedValidity = CacheStrategy.CacheValidity.TRANSIENT,
                ),
                TTLCase(
                    cacheTime = 1_000,
                    currentTime = 2_000,
                    transientTTL = 2000.milliseconds,
                    invalidTTL = 2000.milliseconds,
                    expectedValidity = CacheStrategy.CacheValidity.INVALID,
                ),
            )
        }
    }
}
