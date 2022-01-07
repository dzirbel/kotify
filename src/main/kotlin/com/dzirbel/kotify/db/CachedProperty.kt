package com.dzirbel.kotify.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.OptionalReference
import org.jetbrains.exposed.dao.Reference
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.TransactionManager
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// TODO document and unit test

open class ReadOnlyCachedProperty<V>(private val getter: () -> V) {
    protected var hasCachedValue: Boolean = false
    protected var cachedValue: V? = null

    val cached: V
        @Suppress("unchecked_cast")
        get() = if (hasCachedValue) cachedValue as V else error("no cached value")

    val cachedOrNull: V?
        get() = cachedValue

    val live: V
        get() = getter().also {
            cachedValue = it
            hasCachedValue = true
        }

    fun loadToCache() {
        live
    }

    fun invalidate() {
        hasCachedValue = false
        cachedValue = null
    }
}

class CachedProperty<V>(getter: () -> V, private val setter: (V) -> Unit) : ReadOnlyCachedProperty<V>(getter = getter) {
    fun set(value: V) {
        cachedValue = value
        hasCachedValue = true
        setter(value)
    }
}

fun <T, V> ReadWriteProperty<T, V>.cached(): ReadOnlyProperty<T, CachedProperty<V>> {
    return cached(baseToDerived = { it!! }, derivedToBase = { it!! })
}

// TODO add these transaction checks elsewhere?
fun <T, V> ReadWriteProperty<T, SizedIterable<V>>.cachedAsList(): ReadOnlyProperty<T, CachedProperty<List<V>>> {
    return cached(
        baseToDerived = {
            require(TransactionManager.manager.currentOrNull() != null)
            it.toList()
        },
        derivedToBase = {
            require(TransactionManager.manager.currentOrNull() != null)
            SizedCollection(it)
        },
    )
}

fun <T, Base, Derived> ReadWriteProperty<T, Base>.cached(
    baseToDerived: (Base) -> Derived,
    derivedToBase: (Derived) -> Base,
): ReadOnlyProperty<T, CachedProperty<Derived>> {
    val delegate = this
    return object : ReadOnlyProperty<T, CachedProperty<Derived>> {
        var prop: CachedProperty<Derived>? = null
        override fun getValue(thisRef: T, property: KProperty<*>): CachedProperty<Derived> {
            if (prop == null) {
                prop = CachedProperty(
                    getter = { baseToDerived(delegate.getValue(thisRef, property)) },
                    setter = { value -> delegate.setValue(thisRef, property, derivedToBase(value)) },
                )
            }

            return prop!!
        }
    }
}

fun <T, V> ReadOnlyProperty<T, V>.cachedReadOnly(): ReadOnlyProperty<T, ReadOnlyCachedProperty<V>> {
    return cachedReadOnly { it }
}

fun <T, Base, Derived> ReadOnlyProperty<T, Base>.cachedReadOnly(
    baseToDerived: (Base) -> Derived,
): ReadOnlyProperty<T, ReadOnlyCachedProperty<Derived>> {
    val delegate = this
    return object : ReadOnlyProperty<T, ReadOnlyCachedProperty<Derived>> {
        var prop: ReadOnlyCachedProperty<Derived>? = null
        override fun getValue(thisRef: T, property: KProperty<*>): ReadOnlyCachedProperty<Derived> {
            if (prop == null) {
                prop = ReadOnlyCachedProperty(
                    getter = { baseToDerived(delegate.getValue(thisRef, property)) },
                )
            }

            return prop!!
        }
    }
}

fun <
    REF : Comparable<REF>,
    BID : Comparable<BID>,
    Base : Entity<BID>,
    TID : Comparable<TID>,
    Target : Entity<TID>,
    > Reference<REF, TID, Target>.cached(): ReadOnlyProperty<Base, CachedProperty<Target>> {
    val delegate = this
    return object : ReadOnlyProperty<Base, CachedProperty<Target>> {
        var prop: CachedProperty<Target>? = null
        override fun getValue(thisRef: Base, property: KProperty<*>): CachedProperty<Target> {
            if (prop == null) {
                prop = CachedProperty(
                    getter = {
                        with(thisRef) { delegate.getValue(thisRef, property) }
                    },
                    setter = { value ->
                        with(thisRef) { delegate.setValue(thisRef, property, value) }
                    },
                )
            }

            return prop!!
        }
    }
}

fun <
    REF : Comparable<REF>,
    BID : Comparable<BID>,
    Base : Entity<BID>,
    TID : Comparable<TID>,
    Target : Entity<TID>,
    > OptionalReference<REF, TID, Target>.cached(): ReadOnlyProperty<Base, CachedProperty<Target?>> {
    val delegate = this
    return object : ReadOnlyProperty<Base, CachedProperty<Target?>> {
        var prop: CachedProperty<Target?>? = null
        override fun getValue(thisRef: Base, property: KProperty<*>): CachedProperty<Target?> {
            if (prop == null) {
                prop = CachedProperty(
                    getter = {
                        with(thisRef) { delegate.getValue(thisRef, property) }
                    },
                    setter = { value ->
                        with(thisRef) { delegate.setValue(thisRef, property, value) }
                    },
                )
            }

            return prop!!
        }
    }
}
