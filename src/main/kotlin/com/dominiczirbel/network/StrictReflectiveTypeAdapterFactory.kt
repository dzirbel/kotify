package com.dominiczirbel.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.ConstructorConstructor
import com.google.gson.internal.`$Gson$Types`
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * A [TypeAdapterFactory] which overwrites the default [ReflectiveTypeAdapterFactory], which provides type adapters for
 * user-defined classes by reflecting over its fields.
 *
 * The [TypeAdapter]s provided by this factory are more strict than the default [ReflectiveTypeAdapterFactory.Adapter]s;
 * they require the JSON and the classes have one-to-one fields.
 *
 * When [requireAllClassFieldsUsed] is true, every member field of a class being instantiated are required to be present
 * in the JSON, otherwise a [JsonSyntaxException] is thrown when parsing.
 *
 * When [requireAllJsonFieldsUsed] is true, every field in the JSON must be read into a class field, otherwise a
 * [JsonSyntaxException] is thrown when parsing.
 */
class StrictReflectiveTypeAdapterFactory(
    private val requireAllClassFieldsUsed: Boolean = true,
    private val requireAllJsonFieldsUsed: Boolean = true,
    private val allowUnusedNulls: Boolean = true
) : TypeAdapterFactory {

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class OptionalField

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val delegate = gson.getDelegateAdapter(this, type)
        return (delegate as? ReflectiveTypeAdapterFactory.Adapter)
            ?.let {
                StrictReflectiveTypeAdapter(
                    gson = gson,
                    type = type,
                    delegate = it,
                    requireAllJsonFieldsUsed = requireAllJsonFieldsUsed,
                    requireAllClassFieldsUsed = requireAllClassFieldsUsed,
                    allowUnusedNulls = allowUnusedNulls
                )
            }
    }
}

private class StrictReflectiveTypeAdapter<T>(
    private val gson: Gson,
    private val type: TypeToken<T>,
    private val delegate: ReflectiveTypeAdapterFactory.Adapter<T>,
    private val requireAllJsonFieldsUsed: Boolean,
    private val requireAllClassFieldsUsed: Boolean,
    private val allowUnusedNulls: Boolean
) : TypeAdapter<T>() {
    /**
     * The member fields of [T]; paired by [KProperty1] (Kotlin reflection) and [Field] (Java reflection); only
     * including fields which have a non-null [Field], i.e. those which have a backing field.
     */
    private val fields: List<Pair<KProperty1<in T, *>, Field>> by lazy {
        type.rawType.kotlin.memberProperties.mapNotNull { kProperty ->
            kProperty.javaField?.let { javaField -> Pair(kProperty, javaField) }
        }
    }

    /**
     * The member fields of [T] which are required when [requireAllClassFieldsUsed] is true, i.e. the subset of [fields]
     * which are non-nullable and not annotated with [StrictReflectiveTypeAdapterFactory.OptionalField].
     */
    private val requiredFields: List<Field> by lazy {
        fields
            .filter { (kProperty, _) ->
                !kProperty.returnType.isMarkedNullable &&
                    !kProperty.hasAnnotation<StrictReflectiveTypeAdapterFactory.OptionalField>()
            }
            .map { it.second }
    }

    private val constructor by lazy { ConstructorConstructor(emptyMap()).get(type) }

    /**
     * Returns a [Set] of the serialized names for this class [Field]; JSON fields which match any of these names should
     * be serialized into this [Field].
     */
    private val Field.serializedNames: Set<String>
        get() {
            return getAnnotation(SerializedName::class.java)?.let { annotation ->
                setOfNotNull(annotation.value).plus(annotation.alternate)
            } ?: setOf(gson.fieldNamingStrategy().translateName(this))
        }

    /**
     * Determines the [Field] which should be set for the JSON field of the given [name].
     */
    private fun fieldFor(name: String): Field? {
        return fields.find { (_, javaField) -> javaField.serializedNames.contains(name) }?.second
    }

    override fun write(out: JsonWriter, value: T) {
        delegate.write(out, value)
    }

    override fun read(reader: JsonReader): T? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val instance = constructor.construct()
        val missingFields = if (requireAllClassFieldsUsed) requiredFields.toMutableSet() else null

        try {
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val field = fieldFor(name) ?: run {
                    if (requireAllJsonFieldsUsed && (allowUnusedNulls || reader.peek() != JsonToken.NULL)) {
                        val jsonValue = gson.getAdapter(Any::class.java).read(reader)
                        throw JsonSyntaxException(
                            "Model class ${type.type.typeName} does not contain a field for JSON property " +
                                "`$name` with value `$jsonValue`"
                        )
                    } else {
                        null
                    }
                }

                // skip past fields on JSON which have no class field when requireAllJsonFieldsUsed is false
                field ?: continue

                if (!field.trySetAccessible()) {
                    throw JsonSyntaxException("Could not make $field accessible")
                }

                val fieldType = `$Gson$Types`.resolve(type.type, type.rawType, field.genericType)
                val adapter = gson.getAdapter(TypeToken.get(fieldType))
                val fieldValue = adapter.read(reader)

                field[instance] = fieldValue
                missingFields?.remove(field)
            }
        } catch (e: IllegalStateException) {
            throw JsonSyntaxException(e)
        }
        reader.endObject()

        if (missingFields?.isNotEmpty() == true) {
            throw JsonSyntaxException(
                "Model class ${type.type.typeName} required field(s) which were not present in the JSON: " +
                    missingFields.joinToString { it.serializedNames.toString() }
            )
        }

        return instance
    }
}
