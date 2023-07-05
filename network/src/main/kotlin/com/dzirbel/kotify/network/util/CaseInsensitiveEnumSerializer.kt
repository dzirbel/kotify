package com.dzirbel.kotify.network.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale
import kotlin.reflect.KClass

/**
 * A [KSerializer] which serializes and deserializes [Enum]s based on their [Enum.name], ignoring case.
 *
 * The default serializer requires that the decoded value matches the enum value's name (or its SerialName) exactly, and
 * so it cannot handle cases where the JSON value may be sometimes uppercase and sometimes lowercase.
 *
 * Also optionally allows providing a [fallbackValue] which will be used if deserialization fails.
 */
internal abstract class CaseInsensitiveEnumSerializer<E : Enum<E>>(
    private val enumClass: KClass<E>,
    private val fallbackValue: E? = null,
) : KSerializer<E> {
    override val descriptor = PrimitiveSerialDescriptor(enumClass.simpleName.orEmpty(), PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): E {
        val stringValue = decoder.decodeString().uppercase(Locale.getDefault())
        return if (fallbackValue != null) {
            @Suppress("SwallowedException")
            try {
                java.lang.Enum.valueOf(enumClass.java, stringValue)
            } catch (ex: IllegalArgumentException) {
                // TODO log unexpected value?
                fallbackValue
            }
        } else {
            java.lang.Enum.valueOf(enumClass.java, stringValue)
        }
    }
}
