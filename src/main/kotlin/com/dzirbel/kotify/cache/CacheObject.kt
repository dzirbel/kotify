package com.dzirbel.kotify.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

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
     * The data being cached.
     */
    val obj: Any,

    /**
     * The time the object was cached.
     */
    val cacheTime: Long = System.currentTimeMillis(),

    /**
     * The type of the cached [obj], i.e. the [java.lang.Class.getTypeName] of its class.
     *
     * This is used to determine the class to be deserialized at runtime.
     */
    val type: String = obj::class.java.typeName,

    /**
     * A hash of the [obj]'s class, i.e. [java.lang.Class.hashCode].
     *
     * This is used to verify that the underlying class is the same at deserialization time as it was at serialization
     * time; if they conflict a [CacheObject.Serializer.ClassHashChangedException] will be thrown.
     */
    val classHash: Int = obj::class.hashFields()
) {
    companion object {
        private val hashes: MutableMap<KClass<*>, Int> = mutableMapOf()

        /**
         * Creates a hash of the fields and methods of this [KClass], to verify that the fields are the same when
         * deserializing as when they were serialized.
         *
         * Note that neither [KClass] nor [Class] provides a [hashCode] with these semantics, so this custom
         * implementation is necessary.
         */
        internal fun KClass<*>.hashFields(): Int {
            return hashes.getOrPut(this) {
                val fields = java.fields
                    .map { field -> field.name + field.type.canonicalName }
                    .sorted()
                val methods = java.methods
                    .map { method -> method.name + method.parameters.joinToString { it.name + it.type.canonicalName } }
                    .sorted()

                @Suppress("UnnecessaryParentheses", "MagicNumber")
                fields.hashCode() + (13 * methods.hashCode())
            }
        }
    }

    /**
     * A custom [KSerializer] which uses [type] to deserialize [obj] to the appropriate class.
     *
     * If there is an error deserializing [obj] it will be set instead to a [Throwable].
     */
    @Suppress("MagicNumber")
    class Serializer : KSerializer<CacheObject> {
        class ClassHashChangedException(originalHash: Int, deserializedHash: Int, type: String) : Throwable(
            "Found conflicting class hashes for $type : " +
                "cached as $originalHash but attempting to deserialize with $deserializedHash"
        )

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

        override fun serialize(encoder: Encoder, value: CacheObject) {
            val objClass = value.obj::class
            require(objClass.java.typeName == value.type)
            val objSerializer = objClass.serializer()

            encoder.encode {
                encodeStringElement(descriptor, 0, value.id)
                encodeLongElement(descriptor, 1, value.cacheTime)
                encodeStringElement(descriptor, 2, value.type)
                encodeIntElement(descriptor, 3, value.classHash)

                @Suppress("UNCHECKED_CAST")
                encodeSerializableElement(descriptor, 4, objSerializer as SerializationStrategy<Any>, value.obj)
            }
        }

        override fun deserialize(decoder: Decoder): CacheObject {
            return decoder.decode {
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

                            val serializer = try {
                                val cls = Class.forName(type).kotlin

                                if (cls.hashFields() != classHash) {
                                    throw ClassHashChangedException(
                                        originalHash = classHash,
                                        deserializedHash = cls.hashCode(),
                                        type = type
                                    )
                                }

                                cls.serializer()
                            } catch (ex: Throwable) {
                                // continue past this element with a generic serializer
                                decodeSerializableElement(descriptor, index, JsonObject::class.serializer())

                                obj = ex
                                null
                            }

                            serializer?.let { obj = decodeSerializableElement(descriptor, index, it) }
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

        /**
         * Encodes the structure described by [descriptor].
         *
         * Avoids using [kotlinx.serialization.encoding.encodeStructure] since it swallows exceptions in [block] if the
         * [CompositeEncoder.endStructure] calls also throws an error, which it typically does if [block] fails.
         */
        private fun Encoder.encode(block: CompositeEncoder.() -> Unit) {
            val composite = beginStructure(descriptor)
            composite.block()
            composite.endStructure(descriptor)
        }

        /**
         * Decodes the structure described by [descriptor].
         *
         * Avoids using [kotlinx.serialization.encoding.decodeStructure] since it swallows exceptions in [block] if the
         * [CompositeDecoder.endStructure] calls also throws an error, which it typically does if [block] fails.
         */
        private fun <T> Decoder.decode(block: CompositeDecoder.() -> T): T {
            val composite = beginStructure(descriptor)
            return composite.block().also { composite.endStructure(descriptor) }
        }
    }
}
