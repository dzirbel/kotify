package com.dominiczirbel.cache

import com.dominiczirbel.cache.Cache.TTLStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import kotlin.reflect.KClass

/**
 * An object which can be added to a [Cache] which provides its own [id] and an optional set of additional
 * [cacheableObjects] which are associated with it and should also be cached.
 *
 * This allows for recursive addition of objects attached to a single object, for example if fetching an album also
 * returns a list of its tracks, the track objects can be individually cached as well.
 *
 * TODO don't save associated objects as part of the cache
 */
interface CacheableObject {
    /**
     * The ID of the cached object; if null it will not be cached.
     */
    val id: String?

    /**
     * An optional collection of associated objects which should also be cached alongside this [CacheableObject].
     *
     * Should NOT include this [CacheableObject].
     */
    val cacheableObjects: Collection<CacheableObject>
        get() = emptySet()

    /**
     * Recursively finds all associated [CacheableObject] from this [CacheableObject] and its [cacheableObjects].
     *
     * Note that this will loop infinitely if there is a cycle of [CacheableObject]s, so associations must be acyclic.
     */
    val recursiveCacheableObjects: Collection<CacheableObject>
        get() = cacheableObjects.flatMap { it.recursiveCacheableObjects }.plus(this)
}

/**
 * A wrapper class around a cached object [obj], with caching metadata and a custom [CacheObject.Serializer] to
 * serialize arbitrary values and store their [type] for deserialization.
 *
 * Cached objects must themselves be [Serializable].
 */
@Serializable(with = CacheObject.Serializer::class)
data class CacheObject(
    /**
     * The cached object's ID, unique among objects in the [Cache].
     *
     * The object ID is used for arbitrary lookup, i.e. [Cache.get].
     */
    val id: String,

    /**
     * The time the object was cached.
     */
    val cacheTime: Long = System.currentTimeMillis(),

    /**
     * The type of the cached [obj], i.e. the [java.lang.Class.getTypeName] of its class.
     *
     * This is used to determine the class to be deserialized at runtime.
     */
    val type: String,

    /**
     * A hash of the [obj]'s class, i.e. [java.lang.Class.hashCode].
     *
     * This is used to verify that the underlying class is the same at deserialization time as it was at serialization
     * time; if they conflict a [CacheObject.Serializer.ClassHashChangedException] will be thrown.
     */
    val classHash: Int,

    /**
     * The data being cached.
     */
    val obj: Any
) {
    constructor(id: String, obj: Any, cacheTime: Long = System.currentTimeMillis()) : this(
        id = id,
        cacheTime = cacheTime,
        type = obj::class.java.typeName,
        classHash = obj::class.hashCode(),
        obj = obj
    )

    /**
     * A custom [KSerializer] which uses [type] to deserialize [obj] to the appropriate class.
     */
    @Suppress("MagicNumber")
    class Serializer : KSerializer<CacheObject> {
        class ClassHashChangedException(originalHash: Int, deserializedHash: Int, type: String) : Throwable(
            "Found conflicting class hashes for $type : " +
                "cached as $originalHash but attempting to deserialize with $deserializedHash"
        )

        @InternalSerializationApi
        @ExperimentalSerializationApi
        override val descriptor = buildClassSerialDescriptor("CacheObject") {
            element("id", PrimitiveSerialDescriptor("id", PrimitiveKind.STRING))
            element("cacheTime", PrimitiveSerialDescriptor("cacheInt", PrimitiveKind.LONG))
            element("type", PrimitiveSerialDescriptor("type", PrimitiveKind.STRING))
            element("classHash", PrimitiveSerialDescriptor("classHash", PrimitiveKind.INT))

            // this doesn't seem quite accurate (it's not actually a ContextualSerializer, just whatever runtime
            // serializer is available for the class), but seems to work - possibly because it's never really used
            element(
                "obj",
                buildSerialDescriptor("kotlinx.serialization.ContextualSerializer", SerialKind.CONTEXTUAL)
            )
        }

        @InternalSerializationApi
        @ExperimentalSerializationApi
        override fun serialize(encoder: Encoder, value: CacheObject) {
            val objClass = value.obj::class
            require(objClass.java.typeName == value.type)
            val objSerializer = objClass.serializer()

            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.id)
                encodeLongElement(descriptor, 1, value.cacheTime)
                encodeStringElement(descriptor, 2, value.type)
                encodeIntElement(descriptor, 3, value.classHash)

                @Suppress("UNCHECKED_CAST")
                encodeSerializableElement(descriptor, 4, objSerializer as SerializationStrategy<Any>, value.obj)
            }
        }

        @ExperimentalSerializationApi
        @InternalSerializationApi
        override fun deserialize(decoder: Decoder): CacheObject {
            return decoder.decodeStructure(descriptor) {
                var id: String? = null
                var cacheTime: Long? = null
                var type: String? = null
                var classHash: Int? = null
                var obj: Any? = null

                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeStringElement(descriptor, index)
                        1 -> cacheTime = decodeLongElement(descriptor, index)
                        2 -> type = decodeStringElement(descriptor, index)
                        3 -> classHash = decodeIntElement(descriptor, index)
                        4 -> {
                            requireNotNull(type) { "attempting to deserialize obj before type" }
                            requireNotNull(classHash) { "attempting to deserialize obj before classHash" }

                            val cls = Class.forName(type).kotlin
                            if (cls.hashCode() != classHash) {
                                throw ClassHashChangedException(
                                    originalHash = classHash,
                                    deserializedHash = cls.hashCode(),
                                    type = type
                                )
                            }
                            val serializer = cls.serializer()
                            obj = decodeSerializableElement(descriptor, index, serializer)
                        }
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }

                CacheObject(
                    id = requireNotNull(id) { "never deserialized id" },
                    cacheTime = requireNotNull(cacheTime) { "never deserialized cacheTime" },
                    type = requireNotNull(type) { "never deserialized type" },
                    classHash = requireNotNull(classHash) { "never deserialized classHash" },
                    obj = requireNotNull(obj) { "never deserialized obj" }
                )
            }
        }
    }
}

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
 * TODO thread safety
 */
class Cache(
    private val file: File,
    private val ttlStrategy: TTLStrategy = TTLStrategy.AlwaysValid,
    private val replacementStrategy: ReplacementStrategy = ReplacementStrategy.AlwaysReplace
) {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    private val _cache: MutableMap<String, CacheObject> = mutableMapOf()

    /**
     * The full set of valid, in-memory [CacheObject]s.
     *
     * TODO doesn't actually remove expired objects from the cache, only from the returned map
     */
    val cache: Map<String, CacheObject>
        get() = _cache.filterValues { ttlStrategy.isValid(it) }

    /**
     * Gets the [CacheObject] associated with [id], if it exists in the in-memory cache and is still valid according to
     * [ttlStrategy].
     */
    fun getCached(id: String): CacheObject? {
        return _cache[id]?.let { cacheObject ->
            cacheObject.takeIf { ttlStrategy.isValid(it) } ?: null.also { _cache.remove(id) }
        }
    }

    /**
     * Writes [value] and all its [CacheableObject.recursiveCacheableObjects] to the in-memory cache, using their
     * [CacheableObject.id].
     *
     * Any [CacheableObject]s with a null [CacheableObject.id] will be ignored.
     *
     * If a value is already cached with a certain id, it will be removed as determined by the [replacementStrategy].
     *
     * [cacheTime] is the time the object(s) should be considered cached; by default this is the current system time but
     * may be an arbitrary value, e.g. to reflect a value which was fetched previously and thus may already be
     * out-of-date.
     */
    fun put(value: CacheableObject, cacheTime: Long = System.currentTimeMillis()) {
        listOf(value).plus(value.recursiveCacheableObjects).forEach { cacheableObject ->
            cacheableObject.id?.let { id -> put(id, cacheableObject, cacheTime) }
        }
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
     */
    fun put(id: String, value: Any, cacheTime: Long = System.currentTimeMillis()): Boolean {
        val current = getCached(id)?.obj
        val replace = current?.let { replacementStrategy.replace(it, value) } != false
        if (replace) {
            _cache[id] = CacheObject(id = id, obj = value, cacheTime = cacheTime)
        }

        return replace
    }

    /**
     * Gets the value of type [T] in the cache for [id], or if the value for [id] does not exist or has a type other
     * than [T], fetches a new value from [remote], puts it in the cache, and returns it.
     *
     * Note that if the remotely-fetched value is a [CacheableObject], all of its
     * [CacheableObject.recursiveCacheableObjects] will be added to the cache as well.
     */
    inline fun <reified T : Any> get(id: String, remote: () -> T): T {
        return getCached(id)?.obj as? T
            ?: remote().also { if (it is CacheableObject) put(it) else put(id, it) }
    }

    inline fun <reified T : Any> update(id: String, update: (T?) -> T): T {
        return update(getCached(id)?.obj as? T).also { put(id, it) }
    }

    /**
     * Gets all the valid values in the cache of type [T].
     */
    inline fun <reified T : Any> allOfType(): List<T> {
        return cache.values.mapNotNull { it.obj as? T }
    }

    /**
     * Invalidates the cached value with the given [id], removing it from the cache and returning it.
     */
    fun invalidate(id: String): CacheObject? {
        return _cache.remove(id)
    }

    /**
     * Saves the current in-memory cache to [file] as JSON.
     */
    fun save() {
        val content = json.encodeToString(cache)
        Files.write(file.toPath(), content.split('\n'))
    }

    /**
     * Loads the saved cache from [file] and replaces all current in-memory values with its contents.
     */
    fun load() {
        _cache.clear()
        _cache.putAll(
            FileReader(file).use { it.readLines().joinToString(separator = " ") }
                .let { json.decodeFromString<Map<String, CacheObject>>(it) }
                .filterValues { ttlStrategy.isValid(it) }
        )
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
