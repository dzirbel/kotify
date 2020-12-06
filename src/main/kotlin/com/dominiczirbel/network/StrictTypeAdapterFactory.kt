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

// TODO clean up and document
object StrictTypeAdapterFactory : TypeAdapterFactory {

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class OptionalField

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val delegate = gson.getDelegateAdapter(this, type)
        return (delegate as? ReflectiveTypeAdapterFactory.Adapter)?.let { reflectiveAdapter ->
            object : TypeAdapter<T>() {
                private val fields: Collection<KProperty1<in T, *>> by lazy {
                    type.rawType.kotlin.memberProperties.filter { it.javaField != null }
                }
                private val requiredFields by lazy {
                    fields.filter {
                        !it.returnType.isMarkedNullable && !it.hasAnnotation<OptionalField>()
                    }
                }

                private val constructor by lazy { ConstructorConstructor(emptyMap()).get(type) }

                private val Field.serializedNames: Set<String>
                    get() {
                        return getAnnotation(SerializedName::class.java)?.let { annotation ->
                            setOfNotNull(annotation.value).plus(annotation.alternate)
                        } ?: setOf(gson.fieldNamingStrategy().translateName(this))
                    }

                private fun fieldFor(name: String): KProperty1<in T, *>? {
                    return fields.find { field ->
                        field.javaField?.serializedNames?.contains(name) == true
                    }
                }

                override fun write(out: JsonWriter, value: T) {
                    reflectiveAdapter.write(out, value)
                }

                override fun read(reader: JsonReader): T? {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull()
                        return null // TODO check if T is nullable type?
                    }

                    val instance = constructor.construct()
                    val missingFields = requiredFields.toMutableList()

                    try {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            val field = fieldFor(name) ?: run {
                                val jsonValue = gson.getAdapter(Any::class.java).read(reader)
                                // TODO still require that it exists in the class if it has null value?
                                throw JsonSyntaxException(
                                    "Model class ${type.type.typeName} does not contain a field for JSON property " +
                                        "`$name` with value `$jsonValue`"
                                )
                            }

                            val javaField = field.javaField
                                ?: throw JsonSyntaxException("no backing field for $name")

                            if (!javaField.trySetAccessible()) {
                                throw JsonSyntaxException("could not set $javaField to accessible")
                            }

                            val fieldType = `$Gson$Types`.resolve(type.type, type.rawType, javaField.genericType)
                            val adapter = gson.getAdapter(TypeToken.get(fieldType))
                            val fieldValue = adapter.read(reader)

                            javaField.set(instance, fieldValue)
                            missingFields.remove(field)
                        }
                    } catch (e: IllegalStateException) {
                        throw JsonSyntaxException(e)
                    }
                    reader.endObject()

                    if (missingFields.isNotEmpty()) {
                        throw JsonSyntaxException(
                            "Model class ${type.type.typeName} required field(s) which were not present in the JSON: " +
                                missingFields.joinToString { it.javaField?.serializedNames?.toString() ?: it.name }
                        )
                    }

                    return instance
                }
            }
        }
    }
}
