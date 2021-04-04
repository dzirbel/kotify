package com.dominiczirbel.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dominiczirbel.cache.Cache.TTLStrategy
import kotlinx.coroutines.Deferred
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import kotlin.reflect.KClass
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
 * An optional [ttlStrategy] can limit the values in the cache to observe an arbitrary [TTLStrategy]. Once the
 * [ttlStrategy] marks an object as invalid, it will no longer appear in any of the cache accessors, e.g. [cache],
 * [get], etc., but may only be removed from memory once it is attempted to be accessed.
 *
 * [eventHandler] will be invoked whenever this [Cache] processes a [CacheEvent].
 */
class Cache(
    private val file: File,
    val saveOnChange: Boolean = false,
    private val ttlStrategy: TTLStrategy = TTLStrategy.AlwaysValid,
    private val replacementStrategy: ReplacementStrategy = ReplacementStrategy.AlwaysReplace,
    private val eventHandler: (List<CacheEvent>) -> Unit = { },
    private val onSave: () -> Unit = { }
) {
    private val json = Json {
        encodeDefaults = true
    }

    private val _cache: MutableMap<String, CacheObject> = mutableMapOf()

    private var lastSaveHash: Int? = null

    private val cacheEventQueue = mutableListOf<CacheEvent>()

    /**
     * The full set of valid, in-memory [CacheObject]s.
     */
    val cache: Map<String, CacheObject>
        get() = synchronized(_cache) { removeExpired() }.also { eventHandler(listOf(CacheEvent.Dump(this))) }

    var size by mutableStateOf(0)
        private set

    /**
     * Gets the [CacheObject] associated with [id], if it exists in the in-memory cache and is still valid according to
     * [ttlStrategy].
     *
     * If the value is expired according to [ttlStrategy] null is returned and it is removed from the in-memory cache,
     * but it is _not_ saved to disk, regardless of [saveOnChange]. Such writes to disk would almost always be
     * unnecessary and too costly.
     */
    fun getCached(id: String): CacheObject? {
        return synchronized(_cache) { getCachedInternal(id) }.also { flushCacheEvents() }
    }

    /**
     * Gets the [CacheObject] associated with each [ids], if it exists in the in-memory cache and is still valid
     * according to [ttlStrategy].
     */
    fun getAllCached(ids: List<String>): List<CacheObject?> {
        return synchronized(_cache) {
            ids.map { id -> getCachedInternal(id) }
        }.also { flushCacheEvents() }
    }

    /**
     * Writes [value] and all its [CacheableObject.recursiveCacheableObjects] to the in-memory cache, using their
     * [CacheableObject.id]s, returning true if any values were added or changed in the cache.
     *
     * If a value is already cached with a certain id, it will be removed as determined by the [replacementStrategy].
     *
     * [cacheTime] is the time the object(s) should be considered cached; by default this is the current system time but
     * may be an arbitrary value, e.g. to reflect a value which was fetched previously and thus may already be
     * out-of-date.
     *
     * If [saveOnChange] is true (defaulting to [Cache.saveOnChange]) and any values were added or changed in the cache,
     * [save] will be called.
     */
    fun put(
        value: CacheableObject,
        cacheTime: Long = System.currentTimeMillis(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        return synchronized(_cache) {
            val change = putInternal(value, cacheTime)
            saveInternal(shouldSave = saveOnChange && change)
            change
        }.also { flushCacheEvents() }
    }

    /**
     * Writes all the [values] (and all their recursive [CacheableObject.recursiveCacheableObjects]) to the in-memory
     * cache, using their [CacheableObject.id]s, returning true if any values were added or changed in the cache.
     *
     * If a value is already cached with a certain id, it will be removed as determined by the [replacementStrategy].
     *
     * If [saveOnChange] is true (defaulting to [Cache.saveOnChange]) and any values were added or changed in the cache,
     * [save] will be called.
     */
    fun putAll(values: Iterable<CacheableObject>, saveOnChange: Boolean = this.saveOnChange): Boolean {
        return synchronized(_cache) {
            var change = false
            values.forEach {
                change = putInternal(it) || change
            }
            saveInternal(shouldSave = change && saveOnChange)
            change
        }.also { flushCacheEvents() }
    }

    /**
     * Writes [value] to the in-memory cache, under the given [id].
     *
     * If a value is already cached with the given [id], it will be removed as determined by the [replacementStrategy];
     * if replaced (or if there was no previous cached object), true will be returned; otherwise false will be returned.
     *
     * [cacheTime] is the time the object should be considered cached; by default this is the current system time but
     * may be an arbitrary value, e.g. to reflect a value which was fetched previously and thus may already be
     * out-of-date.
     *
     * If [saveOnChange] is true (defaulting to [Cache.saveOnChange]) and the value was written to the cache, the
     * in-memory cache will be written to disk.
     */
    fun put(
        id: String,
        value: Any,
        cacheTime: Long = System.currentTimeMillis(),
        saveOnChange: Boolean = this.saveOnChange
    ): Boolean {
        return synchronized(_cache) {
            val change = putInternal(id = id, value = value, cacheTime = cacheTime)
            saveInternal(shouldSave = change && saveOnChange)
            change
        }.also { flushCacheEvents() }
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
     * Gets all the values of type [T] in the cache for each of [ids], or if the value for an ID does not exist or has a
     * type other than [T], fetches a new value for it from [remote], puts it and all its
     * [CacheableObject.recursiveCacheableObjects] in the cache, and returns it.
     */
    suspend inline fun <reified T : CacheableObject> getAll(
        ids: List<String>,
        saveOnChange: Boolean = this.saveOnChange,
        remote: (String) -> Deferred<T>
    ): List<T> {
        val cached: List<CacheObject?> = getAllCached(ids = ids)
        check(cached.size == ids.size)

        val jobs = mutableMapOf<Int, Deferred<T>>()
        cached.forEachIndexed { index, cacheObject ->
            if (cacheObject?.obj !is T) {
                jobs[index] = remote(ids[index])
            }
        }

        val newObjects = mutableSetOf<T>()
        return cached
            .mapIndexed { index, cacheObject ->
                cacheObject?.obj as? T
                    ?: jobs.getValue(index).await().also { newObjects.add(it) }
            }
            .also { putAll(newObjects, saveOnChange = saveOnChange) }
    }

    /**
     * Gets all the valid values in the cache of type [T].
     */
    inline fun <reified T : Any> allOfType(): List<T> {
        return cache.values.mapNotNull { it.obj as? T }
    }

    /**
     * Invalidates the cached value with the given [id], removing it from the cache and returning it.
     *
     * If there was a cached value to invalidate and [saveOnChange] is true (defaulting to [Cache.saveOnChange]), the
     * cache will be written to disk.
     */
    fun invalidate(id: String, saveOnChange: Boolean = this.saveOnChange): CacheObject? {
        return synchronized(_cache) {
            _cache.remove(id)
                ?.also { queueCacheEvent(CacheEvent.Invalidate(cache = this, id = id, value = it)) }
                ?.also { saveInternal(shouldSave = saveOnChange) }
        }?.also { flushCacheEvents() }
    }

    /**
     * Clears the cache, both in-memory and on disk.
     */
    fun clear() {
        synchronized(_cache) {
            _cache.clear()
            queueCacheEvent(CacheEvent.Clear(this))
            saveInternal(shouldSave = true)
        }
        flushCacheEvents()
    }

    /**
     * Writes the current in-memory cache to [file] as JSON, removing any values that have expired according to
     * [ttlStrategy].
     */
    fun save() {
        synchronized(_cache) {
            removeExpired()
            saveInternal(shouldSave = true)
        }
        flushCacheEvents()
    }

    /**
     * Loads the saved cache from [file] and replaces all current in-memory values with its contents.
     *
     * Simply clears the cache if the file does not exist.
     */
    fun load() {
        var duration: Duration? = null
        synchronized(_cache) {
            _cache.clear()
            if (file.canRead()) {
                duration = measureTime {
                    _cache.putAll(
                        FileReader(file)
                            .use { it.readLines().joinToString(separator = " ") }
                            .let { json.decodeFromString<Map<String, CacheObject>>(it) }
                            .filterValues { ttlStrategy.isValid(it) }
                    )
                }
            }
            lastSaveHash = _cache.hashCode()
        }

        duration?.let {
            eventHandler(listOf(CacheEvent.Load(cache = this, duration = it, file = file)))
        }
    }

    /**
     * Removes values from [_cache] which are expired according to [ttlStrategy], and returns a copy of [_cache] with
     * the expired values removed.
     *
     * Must be externally synchronized on [_cache].
     */
    private fun removeExpired(): Map<String, CacheObject> {
        val filtered = mutableMapOf<String, CacheObject>()
        var anyFiltered = false
        for (entry in _cache) {
            if (ttlStrategy.isValid(entry.value)) {
                filtered[entry.key] = entry.value
            } else {
                anyFiltered = true
            }
        }

        if (anyFiltered) {
            _cache.clear()
            _cache.putAll(filtered)
        }

        return filtered
    }

    /**
     * Gets the cached value under [id], returning null and removing it from the in-memory cache if it is expired
     * according to [ttlStrategy].
     *
     * Must be externally synchronized on [_cache] and usages should call [flushCacheEvents].
     */
    private fun getCachedInternal(id: String): CacheObject? {
        val value = _cache[id]
        if (value == null) {
            queueCacheEvent(CacheEvent.Miss(this, id))
        }

        return value?.let { cacheObject ->
            cacheObject.takeIf { ttlStrategy.isValid(it) }
                ?.also { queueCacheEvent(CacheEvent.Hit(cache = this, id = id, value = it)) }
                ?: null.also { _cache.remove(id) }
        }
    }

    /**
     * Puts [value] in the in-memory cache under [id] if the [replacementStrategy] allows it, returning true if the
     * value was added or changed.
     *
     * Must be externally synchronized on [_cache] and usages should call [flushCacheEvents].
     */
    private fun putInternal(
        id: String,
        value: Any,
        cacheTime: Long = System.currentTimeMillis()
    ): Boolean {
        val current = getCachedInternal(id)?.obj
        val replace = current?.let { replacementStrategy.replace(it, value) } != false
        if (replace) {
            val previous = _cache[id]
            val new = CacheObject(id = id, obj = value, cacheTime = cacheTime)
            _cache[id] = new

            queueCacheEvent(CacheEvent.Update(this, id, previous = previous, new = new))
        }

        return replace
    }

    /**
     * Puts [value] and all its [CacheableObject.recursiveCacheableObjects] in the in-memory cache if the
     * [replacementStrategy] allows it, returning true if any value was added or changed.
     *
     * Must be externally synchronized on [_cache] and usages should call [flushCacheEvents].
     */
    private fun putInternal(value: CacheableObject, cacheTime: Long = System.currentTimeMillis()): Boolean {
        var change = false
        listOf(value).plus(value.recursiveCacheableObjects).forEach { cacheableObject ->
            cacheableObject.id?.let { id ->
                change = putInternal(id = id, value = cacheableObject, cacheTime = cacheTime) || change
            }
        }
        return change
    }

    /**
     * Writes the current in-memory cache to [file] as JSON if [shouldSave] is true (and it has changed since the last
     * save/load).
     *
     * Must be externally synchronized on [_cache] and usages should call [flushCacheEvents].
     */
    private fun saveInternal(shouldSave: Boolean) {
        if (shouldSave) {
            val currentHash = _cache.hashCode()
            if (currentHash != lastSaveHash) {
                // TODO move out of synchronized block
                val duration = measureTime {
                    val content = json.encodeToString(_cache)
                    Files.writeString(file.toPath(), content)
                }

                queueCacheEvent(CacheEvent.Save(cache = this, duration = duration, file = file))
                lastSaveHash = currentHash
            }
        }
    }

    /**
     * Queues [event] to the [cacheEventQueue], to be later flushed by [flushCacheEvents].
     *
     * This avoids calling the [eventHandler] in performance-critical sections of code (i.e. sections which are
     * synchronized on the cache object) and allows batching events for cases where many operations happen at once.
     */
    private fun queueCacheEvent(event: CacheEvent) {
        synchronized(cacheEventQueue) {
            cacheEventQueue.add(event)
        }
    }

    /**
     * Calls the [eventHandler] for each event in the [cacheEventQueue] and clears it.
     */
    private fun flushCacheEvents() {
        val saved: Boolean
        synchronized(cacheEventQueue) {
            // TODO performance
            saved = cacheEventQueue.any { it is CacheEvent.Save }
            eventHandler(cacheEventQueue.toList())
            cacheEventQueue.clear()
        }

        size = _cache.size

        if (saved) {
            onSave()
        }
    }

    /**
     * A generic strategy for determining whether new values should replace existing cached values.
     */
    interface ReplacementStrategy {
        /**
         * Determines whether the [current] object should be replaced by the [new] value.
         */
        fun replace(current: Any, new: Any): Boolean

        /**
         * A [TTLStrategy] which always replaces cached values.
         */
        object AlwaysReplace : ReplacementStrategy {
            override fun replace(current: Any, new: Any) = true
        }

        /**
         * A [TTLStrategy] which never replaces cached values.
         */
        object NeverReplace : ReplacementStrategy {
            override fun replace(current: Any, new: Any) = false
        }
    }

    /**
     * A generic strategy for determining whether cached values are still valid; typically cached values may become
     * invalid after a certain amount of time in the cache.
     */
    interface TTLStrategy {
        fun isValid(cacheObject: CacheObject): Boolean = isValid(cacheObject.cacheTime, cacheObject.obj)

        /**
         * Determines whether the given [obj], cached at [cacheTime], is still valid.
         */
        fun isValid(cacheTime: Long, obj: Any): Boolean

        /**
         * A [TTLStrategy] which always marks cache elements as valid, so they will never be evicted.
         */
        object AlwaysValid : TTLStrategy {
            override fun isValid(cacheTime: Long, obj: Any) = true
        }

        /**
         * A [TTLStrategy] which never marks cache elements as valid, so they will always be fetched from a remote
         * source. This is equivalent to not having a cache.
         */
        object NeverValid : TTLStrategy {
            override fun isValid(cacheTime: Long, obj: Any) = false
        }

        /**
         * A [TTLStrategy] with a [ttl] applied to all cache elements; elements in teh cache for more that [ttl]
         * milliseconds will be evicted.
         */
        class UniversalTTL(private val ttl: Long) : TTLStrategy {
            override fun isValid(cacheTime: Long, obj: Any): Boolean {
                return cacheTime + ttl >= System.currentTimeMillis()
            }
        }

        /**
         * A [TTLStrategy] with a TTL applied class-by-class to cache elements, so elements of certain classes may
         * persist longer in the cache than elements of other classes.
         *
         * [defaultTTL] is used for elements of classes that are not in [classMap]; if null and a cached element is not
         * in [classMap], an exception will be thrown.
         */
        class TTLByClass(
            private val classMap: Map<KClass<*>, Long>,
            private val defaultTTL: Long? = null
        ) : TTLStrategy {
            override fun isValid(cacheTime: Long, obj: Any): Boolean {
                val ttl = classMap[obj::class] ?: defaultTTL
                requireNotNull(ttl) { "no TTL for class ${obj::class}" }

                return cacheTime + ttl >= System.currentTimeMillis()
            }
        }
    }
}
