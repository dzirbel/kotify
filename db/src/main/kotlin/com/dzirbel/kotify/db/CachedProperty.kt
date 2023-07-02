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

/**
 * A wrapper around a property which allows access via either a [live] or [cached] version.
 *
 * This is typically used to wrap properties which are loaded from the database, allowing a convenient way to cache the
 * value in memory for fast access from UI components. This allows the value to become outdated in the database, but
 * hitting the database must be avoided e.g. in composable functions.
 *
 * This is the read-only base class, which reads from [getter] when accessing the [live] value but cannot set a value,
 * unlike [ReadWriteCachedProperty].
 *
 * For convenience, this class has a built-in check [requireGetInTransaction] which asserts that calls to [getter] are
 * done from a transaction. This avoids hard-to-interpret NPEs in many cases.
 */
open class ReadOnlyCachedProperty<V>(private val requireGetInTransaction: Boolean = true, private val getter: () -> V) {
    /**
     * Whether [cachedValue] can currently be used as a cached value, in case [V] is nullable and null is a valid cached
     * value. Should be updated when [cachedValue] is set.
     */
    protected var hasCachedValue: Boolean = false

    protected var cachedValue: V? = null

    /**
     * Gets the cached value, throwing an [IllegalStateException] if no value has been cached (i.e. [live] has never
     * been called).
     */
    val cached: V
        @Suppress("UNCHECKED_CAST")
        get() = if (hasCachedValue) cachedValue as V else error("no cached value")

    /**
     * Gets the cached value, returning null if no value has been cached (i.e. [live] has never been called).
     */
    val cachedOrNull: V?
        get() = cachedValue

    /**
     * Retrieves a fresh value from [getter], caches, and returns it. This is a potentially expensive operation; in
     * cases where a previously computed value could suffice, [cached] should be used.
     *
     * If [requireGetInTransaction] is true and no transaction is in context, an [IllegalStateException] will be thrown.
     */
    val live: V
        get() {
            if (requireGetInTransaction) {
                checkNotNull(TransactionManager.manager.currentOrNull()) {
                    "attempted to call getter outside of a transaction"
                }
            }

            return getter().also { value ->
                cachedValue = value
                hasCachedValue = true
            }
        }

    /**
     * Loads the [live] value into the cache. Equivalent to calling [live], but exists to be explicit that the cached
     * value is being prepared but not returned.
     */
    fun loadToCache() {
        live
    }

    /**
     * Invalidates the cached value, clearing it and requiring a new call to [live] before it will be available.
     */
    fun invalidate() {
        hasCachedValue = false
        cachedValue = null
    }
}

/**
 * A wrapper around a property which allows access via either a [live] or [cached] version, and to [set] the value.
 *
 * This is typically used to wrap properties which are loaded from the database, allowing a convenient way to cache the
 * value in memory for fast access from UI components. This allows the value to become outdated in the database, but
 * hitting the database must be avoided e.g. in composable functions.
 *
 * This is the read-write derived class, which allows [set]ing a value, invoking [setter] and updating the cached value.
 *
 * For convenience, this class has built-in checks [requireGetInTransaction] and [requireSetInTransaction] which assert
 * that calls to [getter] and [setter] are done from transactions, respectively. This avoids hard-to-interpret NPEs in
 * many cases.
 */
class ReadWriteCachedProperty<V>(
    requireGetInTransaction: Boolean = true,
    private val requireSetInTransaction: Boolean = true,
    getter: () -> V,
    private val setter: (V) -> Unit,
) : ReadOnlyCachedProperty<V>(getter = getter, requireGetInTransaction = requireGetInTransaction) {
    /**
     * Sets the value to [value], caching it and invoking [setter].
     *
     * If [requireSetInTransaction] is true and no transaction is in context, an [IllegalStateException] will be thrown.
     */
    fun set(value: V) {
        if (requireSetInTransaction) {
            checkNotNull(TransactionManager.manager.currentOrNull()) {
                "attempted to call setter outside of a transaction for $value"
            }
        }

        cachedValue = value
        hasCachedValue = true
        setter(value)
    }
}

/**
 * Wraps this [ReadWriteProperty] in a [ReadWriteCachedProperty] of the same type.
 */
fun <T, V> ReadWriteProperty<T, V>.cached(
    requireGetInTransaction: Boolean = true,
    requireSetInTransaction: Boolean = true,
): ReadOnlyProperty<T, ReadWriteCachedProperty<V>> {
    return cached(
        requireGetInTransaction = requireGetInTransaction,
        requireSetInTransaction = requireSetInTransaction,
        baseToDerived = { it },
        derivedToBase = { it },
    )
}

/**
 * Wraps this [ReadWriteProperty] in a [ReadWriteCachedProperty] which converts the [SizedIterable] to a [List].
 *
 * This is almost always desired, since the elements of a [SizedIterable] cannot be accessed outside of a database
 * transaction so typically the fully-resolved [List] should be cached.
 */
fun <T, V> ReadWriteProperty<T, SizedIterable<V>>.cachedAsList(
    requireGetInTransaction: Boolean = true,
    requireSetInTransaction: Boolean = true,
): ReadOnlyProperty<T, ReadWriteCachedProperty<List<V>>> {
    return cached(
        requireGetInTransaction = requireGetInTransaction,
        requireSetInTransaction = requireSetInTransaction,
        baseToDerived = { it.toList() },
        derivedToBase = { SizedCollection(it) },
    )
}

/**
 * Wraps this [ReadWriteProperty] of [Base] type in a [ReadWriteCachedProperty] of [Derived] type.
 *
 * This allows converting values from [Base] to [Derived] and vice versa via [baseToDerived] and [derivedToBase], and in
 * particular the converted values of the [Derived] type will be cached so conversion only happens once and can be done
 * within the same transaction as accessing the underlying [ReadWriteProperty].
 */
fun <T, Base, Derived> ReadWriteProperty<T, Base>.cached(
    requireGetInTransaction: Boolean = true,
    requireSetInTransaction: Boolean = true,
    baseToDerived: (Base) -> Derived,
    derivedToBase: (Derived) -> Base,
): ReadOnlyProperty<T, ReadWriteCachedProperty<Derived>> {
    val delegate = this
    return lazyReadOnlyProperty { thisRef, property ->
        ReadWriteCachedProperty(
            requireGetInTransaction = requireGetInTransaction,
            requireSetInTransaction = requireSetInTransaction,
            getter = { baseToDerived(delegate.getValue(thisRef, property)) },
            setter = { value -> delegate.setValue(thisRef, property, derivedToBase(value)) },
        )
    }
}

/**
 * Wraps this [ReadOnlyProperty] in a [ReadOnlyCachedProperty] of the same type.
 */
fun <T, V> ReadOnlyProperty<T, V>.cachedReadOnly(
    requireGetInTransaction: Boolean = true,
): ReadOnlyProperty<T, ReadOnlyCachedProperty<V>> {
    return cachedReadOnly(requireGetInTransaction = requireGetInTransaction, baseToDerived = { it })
}

/**
 * Wraps this [ReadOnlyProperty] of [Base] type in a [ReadOnlyCachedProperty] of [Derived] type.
 *
 * This allows converting values from [Base] to [Derived] and via [baseToDerived], and in particular the converted
 * values of the [Derived] type will be cached so conversion only happens once and can be done within the same
 * transaction as accessing the underlying [ReadOnlyProperty].
 */
fun <T, Base, Derived> ReadOnlyProperty<T, Base>.cachedReadOnly(
    requireGetInTransaction: Boolean = true,
    baseToDerived: (Base) -> Derived,
): ReadOnlyProperty<T, ReadOnlyCachedProperty<Derived>> {
    val delegate = this
    return lazyReadOnlyProperty { thisRef, property ->
        ReadOnlyCachedProperty(
            requireGetInTransaction = requireGetInTransaction,
            getter = { baseToDerived(delegate.getValue(thisRef, property)) },
        )
    }
}

/**
 * Wraps this [Reference] from a [Base] entity to a [Target] entity in a [ReadWriteCachedProperty].
 */
fun <
    REF : Comparable<REF>,
    BaseID : Comparable<BaseID>,
    Base : Entity<BaseID>,
    TargetID : Comparable<TargetID>,
    Target : Entity<TargetID>,
    > Reference<REF, TargetID, Target>.cached(
    requireGetInTransaction: Boolean = true,
    requireSetInTransaction: Boolean = true,
): ReadOnlyProperty<Base, ReadWriteCachedProperty<Target>> {
    val delegate = this
    return lazyReadOnlyProperty { thisRef, property ->
        ReadWriteCachedProperty(
            requireGetInTransaction = requireGetInTransaction,
            requireSetInTransaction = requireSetInTransaction,
            getter = {
                with(thisRef) { delegate.getValue(thisRef, property) }
            },
            setter = { value ->
                with(thisRef) { delegate.setValue(thisRef, property, value) }
            },
        )
    }
}

/**
 * Wraps this [OptionalReference] from a [Base] entity to a [Target] entity in a [ReadWriteCachedProperty].
 */
fun <
    REF : Comparable<REF>,
    BaseID : Comparable<BaseID>,
    Base : Entity<BaseID>,
    TargetID : Comparable<TargetID>,
    Target : Entity<TargetID>,
    > OptionalReference<REF, TargetID, Target>.cached(
    requireGetInTransaction: Boolean = true,
    requireSetInTransaction: Boolean = true,
): ReadOnlyProperty<Base, ReadWriteCachedProperty<Target?>> {
    val delegate = this
    return lazyReadOnlyProperty { thisRef, property ->
        ReadWriteCachedProperty(
            requireGetInTransaction = requireGetInTransaction,
            requireSetInTransaction = requireSetInTransaction,
            getter = {
                with(thisRef) { delegate.getValue(thisRef, property) }
            },
            setter = { value ->
                with(thisRef) { delegate.setValue(thisRef, property, value) }
            },
        )
    }
}

/**
 * Convenience function which returns a [ReadOnlyProperty] whose value is initialized on the first call to [getValue] by
 * [initializer] and re-used on every successive call to [getValue].
 */
private fun <T, V : Any> lazyReadOnlyProperty(
    initializer: (thisRef: T, property: KProperty<*>) -> V,
): ReadOnlyProperty<T, V> {
    return object : ReadOnlyProperty<T, V> {
        var prop: V? = null
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return prop ?: initializer(thisRef, property).also { prop = it }
        }
    }
}
