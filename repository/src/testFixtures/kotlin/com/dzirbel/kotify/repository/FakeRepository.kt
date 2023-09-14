package com.dzirbel.kotify.repository

import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import java.time.Instant

// TODO extract
class FakeLog<T>(override val name: String = "Log") : Log<T> {
    override val writeLock: Mutex = Mutex()
    override val events: List<Log.Event<T>> = emptyList()
    override val eventsFlow: Flow<Log.Event<T>> = emptyFlow()
}

open class FakeRepository<T>(values: Map<String, T> = emptyMap()) : Repository<T> {

    override val log = FakeLog<Repository.LogData>()

    private val cache: MutableMap<String, CacheState<T>> =
        values.mapValues { CacheState.Loaded(it.value, CurrentTime.instant) }.toMutableMap()

    operator fun set(id: String, value: T) {
        set(id, CacheState.Loaded(value, CurrentTime.instant))
    }

    operator fun set(id: String, value: CacheState<T>) {
        cache[id] = value
    }

    override fun stateOf(id: String, cacheStrategy: CacheStrategy<T>): StateFlow<CacheState<T>?> {
        return MutableStateFlow(cache[id])
    }

    override fun statesOf(ids: Iterable<String>, cacheStrategy: CacheStrategy<T>): List<StateFlow<CacheState<T>?>> {
        return ids.map { stateOf(it, cacheStrategy) }
    }

    override fun refreshFromRemote(id: String): Job {
        return Job().apply { complete() }
    }
}

open class FakeEntityRepository<ViewModel : EntityViewModel, Database, Network>(entities: Iterable<ViewModel>) :
    FakeRepository<ViewModel>(values = entities.associateBy { it.id }),
    ConvertingRepository<Database, Network> by FakeConvertingRepository()

fun <T : EntityViewModel> FakeRepository<T>.put(value: T) {
    this[value.id] = value
}

fun <T : EntityViewModel> FakeRepository<T>.put(values: Iterable<T>) {
    for (value in values) put(value)
}

class FakeConvertingRepository<Database, Network> : ConvertingRepository<Database, Network> {
    override fun convertToDB(id: String, networkModel: Network, fetchTime: Instant): Database {
        error("not implemented")
    }
}
