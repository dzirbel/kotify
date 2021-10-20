package com.dzirbel.kotify.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
import com.dzirbel.kotify.util.zipEach
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileReader
import java.util.concurrent.Executors
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * A generic, JSON-based local disk cache for arbitrary objects.
 *
 * Objects are wrapped as [CacheObject]s, which provides some metadata and a way to serialize/deserialize the object as
 * JSON without losing its JVM class.
 *
 * Objects are cached in-memory with no limit, and can be saved to [file] with [save] or loaded from disk (replacing the
 * in-memory cache) with [load].
 *
 * An optional [ttlStrategy] can limit the values in the cache to observe an arbitrary [CacheTTLStrategy]. Once the
 * [ttlStrategy] marks an object as invalid, it will no longer appear in any of the cache accessors, e.g. [cache],
 * [get], etc., but may only be removed from memory once it is attempted to be accessed.
 *
 * [eventHandler] will be invoked whenever this [Cache] processes a [CacheEvent].
 */
class Cache(
    private val file: File,
    val saveOnChange: Boolean = false,
    private val ttlStrategy: CacheTTLStrategy = CacheTTLStrategy.AlwaysValid,
    private val replacementStrategy: CacheReplacementStrategy = CacheReplacementStrategy.AlwaysReplace,
    private val getCurrentTime: () -> Long = { System.currentTimeMillis() },
    private val eventHandler: (List<CacheEvent>) -> Unit = { }
) {
    private val json = Json {
        encodeDefaults = true
    }

    /**
     * A simple wrapper around a [cache] map, which guarantees calls to the map are thread-safe via [modify].
     */
    private class CacheWrapper {
        private val cache: MutableMap<String, CacheObject> = mutableMapOf()

        var size by mutableStateOf(0)
            private set

        /**
         * Modifies the current cache map.
         *
         * The current value of [cache] is passed into [block], which may make modifications to the map and return an
         * arbitrary [T] which is then returned from this function. [T] may not be the same object as [cache] to avoid
         * exposing it to non-thread-safe access. Calls to [block] are synchronized, so as much work as possible should
         * be done outside of it for performance.
         */
        fun <T> modify(block: (MutableMap<String, CacheObject>) -> T): T {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

            return synchronized(this) {
                block(cache)
                    .also { size = cache.size }

                    // Direct references to the cache map should not be exposed; it should always be referenced via the
                    // wrapper. Copes of it may be returned.
                    .also { check(it !== cache) }
            }
        }
    }

    private val cache = CacheWrapper()

    private var lastSaveHash: Int? = null

    private val ioCoroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    /**
     * The number of objects in the cache.
     *
     * This is backed by a [androidx.compose.runtime.MutableState] so it may be used easily in a composition.
     */
    val size: Int
        get() = cache.size

    /**
     * Gets the full set of in-memory cached [CacheObject]s. Typically only used in tests.
     */
    fun getCache(): Map<String, CacheObject> {
        return cache.modify { cacheMap -> cacheMap.removeExpired() }
            .also { eventHandler(listOf(CacheEvent.Dump(this))) }
    }

    /**
     * Gets the [CacheObject] associated with [id], if it exists in the in-memory cache and is still valid according to
     * [ttlStrategy].
     *
     * If the value is expired according to [ttlStrategy] null is returned and it is removed from the in-memory cache,
     * but it is _not_ saved to disk, regardless of [saveOnChange]. Such writes to disk would almost always be
     * unnecessary and too costly.
     */
    fun getCached(id: String): CacheObject? {
        return cache.modify { cacheMap -> cacheMap.getIfValid(id) }
            .also { obj ->
                val event = if (obj == null) {
                    CacheEvent.Miss(cache = this, id = id)
                } else {
                    CacheEvent.Hit(cache = this, id = id, value = obj)
                }
                eventHandler(listOf(event))
            }
    }

    inline fun <reified T> getCachedValue(id: String): T? {
        return getCached(id)?.obj as? T
    }

    /**
     * Gets the [CacheObject] associated with each [ids], if it exists in the in-memory cache and is still valid
     * according to [ttlStrategy].
     */
    fun getCached(ids: Iterable<String>): List<CacheObject?> {
        return cache.modify { cacheMap -> ids.map { id -> cacheMap.getIfValid(id) } }
            .also { objs ->
                val events = objs.zip(ids) { obj, id ->
                    if (obj == null) {
                        CacheEvent.Miss(cache = this, id = id)
                    } else {
                        CacheEvent.Hit(cache = this, id = id, value = obj)
                    }
                }
                eventHandler(events)
            }
    }

    /**
     * A convenience wrapper for [putAll] which writes the given [value] (and all its
     * [CacheableObject.recursiveCacheableObjects]) to the in-memory cache, associated by their [CacheableObject.id]s.
     *
     * Returns true if any value was added or replaced.
     */
    fun put(
        value: CacheableObject,
        cacheTime: Long = getCurrentTime(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        return putAll(values = listOf(value), cacheTime = cacheTime, saveOnChange = saveOnChange)
    }

    /**
     * A convenience wrapper for [putAll] which writes the given [values]s (and all their
     * [CacheableObject.recursiveCacheableObjects]) to the in-memory cache, associated by their [CacheableObject.id]s.
     *
     * Returns true if any values was added or replaced.
     */
    fun putAll(
        values: Iterable<CacheableObject>,
        cacheTime: Long = getCurrentTime(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        val allObjects = values
            .flatMap { it.recursiveCacheableObjects }
            .filter { it.id != null }
            .associateBy { it.id!! }
        return putAll(
            values = allObjects,
            cacheTime = cacheTime,
            saveOnChange = saveOnChange
        )
    }

    /**
     * A convenience wrapper for [putAll] which writes a single [id]-[value] to the in-memory cache.
     *
     * Returns true if the [value] was added or replaced.
     */
    fun put(
        id: String,
        value: Any,
        cacheTime: Long = getCurrentTime(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        return putAll(values = mapOf(id to value), cacheTime = cacheTime, saveOnChange = saveOnChange)
    }

    /**
     * Writes [values] to the in-memory cache, as a map from ID to the cached value.
     *
     * If a value is already cached with the associated, it will be removed as determined by the [replacementStrategy];
     * if any value is added or replaced true will be returned, otherwise false will be returned.
     *
     * [cacheTime] is the time each object should be considered cached; by default this is the current system time but
     * may be an arbitrary value, e.g. to reflect a value which was fetched previously and thus may already be
     * out-of-date.
     *
     * If [saveOnChange] is true (defaulting to [Cache.saveOnChange]) and the value was written to the cache, the
     * in-memory cache will be written to disk in the background.
     */
    fun putAll(
        values: Map<String, Any>,
        cacheTime: Long = getCurrentTime(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        val updates = mutableMapOf<String, Pair<CacheObject?, CacheObject>>()
        val cacheMap = cache.modify { cacheMap ->
            values.forEach { (id, value) ->
                val current = cacheMap.getIfValid(id)?.obj
                if (value != current) {
                    val replace = current?.let { replacementStrategy.replace(current, value) } != false
                    if (replace) {
                        val previous = cacheMap[id]
                        val new = CacheObject(id = id, obj = value, cacheTime = cacheTime)
                        cacheMap[id] = new

                        updates[id] = Pair(previous, new)
                    }
                }
            }

            // only create map copy if it will be used
            if (saveOnChange && updates.isNotEmpty()) HashMap(cacheMap) else null
        }

        if (updates.isNotEmpty()) {
            eventHandler(
                updates.map { (id, update) ->
                    CacheEvent.Update(cache = this, id = id, previous = update.first, new = update.second)
                }
            )

            if (saveOnChange) {
                writeInBackground(cacheMap = cacheMap!!)
            }
        }

        return updates.isNotEmpty()
    }

    /**
     * Gets the value of type [T] in the cache for [id], or if the value for [id] does not exist or has a type other
     * than [T], fetches a new value from [remote], puts it in the cache, and returns it.
     *
     * If the remotely-fetched value is a [CacheableObject], all of its [CacheableObject.recursiveCacheableObjects] will
     * be added to the cache as well.
     */
    inline fun <reified T : Any> get(id: String, saveOnChange: Boolean = this.saveOnChange, remote: () -> T): T {
        return getCached(id)?.obj as? T
            ?: remote().also {
                if (it is CacheableObject) {
                    put(it, saveOnChange = saveOnChange)
                } else {
                    put(id, it, saveOnChange = saveOnChange)
                }
            }
    }

    /**
     * Gets all the values of type [T] in the cache for each of [ids].
     *
     * [remote] will be called with [ids] which do not have a cached value of type [T]; it should return a list of the
     * [T] values associated with each ID. These values will be added to the cache and returned along with the cached
     * values, associated by the given [ids] list.
     */
    inline fun <reified T : CacheableObject> getAll(
        ids: List<String>,
        saveOnChange: Boolean = this.saveOnChange,
        remote: (List<String>) -> List<T>
    ): List<T> {
        val cached: List<CacheObject?> = getCached(ids = ids)
        check(cached.size == ids.size)

        // if we have all the objects cached no need to call remote
        if (cached.all { it?.obj as? T != null }) {
            return cached.map { it?.obj as T }
        }

        val missingIds = mutableListOf<String>()
        cached.zipEach(ids) { cacheObject, id ->
            if (cacheObject?.obj !is T) {
                missingIds.add(id)
            }
        }

        val missingObjects: List<T> = remote(missingIds)

        putAll(missingObjects, saveOnChange = saveOnChange)

        val missingObjectsById: Map<String, T> = missingIds.zipToMap(missingObjects)

        return cached.zip(ids) { cacheObject, id ->
            cacheObject?.obj as? T ?: missingObjectsById.getValue(id)
        }
    }

    /**
     * Invalidates the cached value with the given [id], removing it from the cache and returning it.
     *
     * If there was a cached value to invalidate and [saveOnChange] is true (defaulting to [Cache.saveOnChange]), the
     * cache will be written to disk in the background.
     */
    fun invalidate(id: String, saveOnChange: Boolean = this.saveOnChange): CacheObject? {
        return invalidate(ids = listOf(id), saveOnChange = saveOnChange).first()
    }

    /**
     * Invalidates the cached values with the given [ids], removing them from the cache and returning them.
     *
     * If there were any cached values to invalidate and [saveOnChange] is true (defaulting to [Cache.saveOnChange]),
     * the cache will be written to disk in the background.
     */
    fun invalidate(ids: List<String>, saveOnChange: Boolean = this.saveOnChange): List<CacheObject?> {
        val previousObjects: List<CacheObject?>
        val anyRemoved: Boolean
        val cacheState = cache.modify { cacheMap ->
            previousObjects = ids.map { cacheMap.remove(it) }
            anyRemoved = previousObjects.any { it != null }

            if (saveOnChange && anyRemoved) HashMap(cacheMap) else null
        }

        if (anyRemoved) {
            if (saveOnChange) {
                writeInBackground(cacheMap = cacheState!!)
            }

            eventHandler(
                previousObjects.mapNotNull { cacheObject ->
                    cacheObject?.let { CacheEvent.Invalidate(cache = this, id = cacheObject.id, value = cacheObject) }
                }
            )
        }

        return previousObjects
    }

    /**
     * Clears the cache, both in-memory and on disk.
     */
    fun clear() {
        cache.modify { cacheMap -> cacheMap.clear() }

        if (saveOnChange) {
            writeInBackground(cacheMap = emptyMap())
        }

        eventHandler(listOf(CacheEvent.Clear(this)))
    }

    /**
     * Writes the current in-memory cache to [file] as JSON, removing any values that have expired according to
     * [ttlStrategy].
     *
     * Writes synchronously (blocking until it finishes) when [synchronous] is true, otherwise dispatchers a new
     * coroutine to write the cache in the background.
     */
    fun save(synchronous: Boolean = true) {
        val cacheMap = cache.modify { cacheMap -> cacheMap.removeExpired() }
        if (synchronous) write(cacheMap) else writeInBackground(cacheMap)
    }

    /**
     * Loads the saved cache from [file] and replaces all current in-memory values with its contents.
     *
     * Simply clears the cache if the file does not exist.
     */
    fun load() {
        var duration: Duration? = null
        val errors: MutableList<Throwable> = mutableListOf()

        cache.modify { cacheMap ->
            cacheMap.clear()
            if (file.canRead()) {
                duration = measureTime {
                    cacheMap.putAll(
                        FileReader(file)
                            .use { it.readLines().joinToString(separator = " ") }
                            .let { json.decodeFromString<Map<String, CacheObject>>(it) }
                            .filterValues {
                                if (it.obj is Throwable) {
                                    errors.add(it.obj)
                                    false
                                } else {
                                    true
                                }
                            }
                            .filterValues { ttlStrategy.isValid(cacheObject = it, currentTime = getCurrentTime()) }
                    )
                }
            }

            lastSaveHash = cacheMap.hashCode()
        }

        duration?.let {
            eventHandler(listOf(CacheEvent.Load(cache = this, duration = it, file = file, errors = errors)))
        }
    }

    /**
     * Removes values from this cache map which are expired according to [ttlStrategy] and returns a copy of this map
     * with the expired values removed.
     */
    private fun MutableMap<String, CacheObject>.removeExpired(): Map<String, CacheObject> {
        val filtered = mutableMapOf<String, CacheObject>()
        var anyFiltered = false
        for (entry in this) {
            if (ttlStrategy.isValid(cacheObject = entry.value, currentTime = getCurrentTime())) {
                filtered[entry.key] = entry.value
            } else {
                anyFiltered = true
            }
        }

        if (anyFiltered) {
            clear()
            putAll(filtered)
        }

        return filtered
    }

    /**
     * Gets the cached value under [id], returning null and removing it from this [MutableMap] if it is expired
     * according to [ttlStrategy].
     */
    private fun MutableMap<String, CacheObject>.getIfValid(id: String): CacheObject? {
        return this[id]?.let { cacheObject ->
            if (ttlStrategy.isValid(cacheObject = cacheObject, currentTime = getCurrentTime())) {
                cacheObject
            } else {
                remove(id)
                null
            }
        }
    }

    /**
     * Launches a coroutine to write the given [cacheMap] to the [cache] file.
     *
     * By using the [ioCoroutineContext], these writes will be executed one at a time.
     */
    private fun writeInBackground(cacheMap: Map<String, CacheObject>) {
        GlobalScope.launch(context = ioCoroutineContext) {
            write(cacheMap = cacheMap)
        }
    }

    /**
     * Synchronously writes the given [cacheMap] to the cache [file], if its hash is different from [lastSaveHash].
     */
    private fun write(cacheMap: Map<String, CacheObject>) {
        assertNotOnUIThread()

        val currentHash = cacheMap.hashCode()
        if (currentHash != lastSaveHash) {
            val duration = measureTime {
                file.outputStream().use { outputStream ->
                    json.encodeToStream(cacheMap, outputStream)
                }
            }

            eventHandler(listOf(CacheEvent.Save(cache = this, duration = duration, file = file)))
            lastSaveHash = currentHash
        }
    }
}
