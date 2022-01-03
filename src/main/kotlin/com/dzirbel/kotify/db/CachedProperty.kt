package com.dzirbel.kotify.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Convenience wrapper for [cachedOutsideTransaction] which wraps a [SizedIterable] property in a cached [List]
 * property, typically for many-to-many joins.
 */
fun <T, E> ReadWriteProperty<T, SizedIterable<E>>.cachedAsList(db: Database? = null): ReadWriteProperty<T, List<E>> {
    return cachedOutsideTransaction(
        baseToDerived = { it.toList() },
        derivedToBase = { SizedCollection(it) },
        db = db,
    )
}

/**
 * Convenience wrapper for [cachedOutsideTransaction] which wraps the same type of property [V].
 */
fun <T, V : Any> ReadWriteProperty<T, V>.cachedIdentity(db: Database? = null): ReadWriteProperty<T, V> {
    return cachedOutsideTransaction(baseToDerived = { it }, derivedToBase = { it }, db = db)
}

/**
 * Returns a [ReadWriteProperty] which delegates to this one, but caches the value to make it available outside of a
 * transaction from [db].
 *
 * This is useful for properties depending on a join where the joined values are unlikely to change, and allows for
 * faster access that does not hit the database in most cases.
 *
 * The cached value may be of a different type [DerivedValue] than the original one [BaseValue], to allow the mapping
 * [baseToDerived] to happen within the transaction (necessary for e.g. accessing the values of [SizedIterable]).
 *
 * TODO unit test
 */
fun <T, BaseValue : Any, DerivedValue : Any> ReadWriteProperty<T, BaseValue>.cachedOutsideTransaction(
    baseToDerived: (BaseValue) -> DerivedValue,
    derivedToBase: (DerivedValue) -> BaseValue,
    db: Database? = null,
): ReadWriteProperty<T, DerivedValue> {
    val delegate = this
    return object : ReadWriteProperty<T, DerivedValue> {
        private var cachedValue: DerivedValue? = null

        private fun inTransaction() = db.transactionManager.currentOrNull() != null

        override fun getValue(thisRef: T, property: KProperty<*>): DerivedValue {
            return if (inTransaction()) {
                baseToDerived(delegate.getValue(thisRef, property))
                    .also { cachedValue = it }
            } else {
                cachedValue?.let { return it }
                transaction(db) { baseToDerived(delegate.getValue(thisRef, property)) }
                    .also { cachedValue = it }
            }
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: DerivedValue) {
            cachedValue = value
            delegate.setValue(thisRef, property, derivedToBase(value))
        }
    }
}

/**
 * Returns a [ReadOnlyProperty] which delegates to this one, but caches the value to make it available outside of a
 * transaction from [db].
 *
 * This is useful for properties depending on a join where the joined values are unlikely to change, and allows for
 * faster access that does not hit the database in most cases.
 *
 * TODO unit test
 */
fun <T, V> ReadOnlyProperty<T, V>.cachedOutsideTransaction(db: Database? = null): ReadOnlyProperty<T, V> {
    val delegate = this
    return object : ReadOnlyProperty<T, V> {
        private var cachedValue: V? = null

        private fun inTransaction() = db.transactionManager.currentOrNull() != null

        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return if (inTransaction()) {
                delegate.getValue(thisRef, property)
                    .also { cachedValue = it }
            } else {
                cachedValue?.let { return it }
                transaction(db) { delegate.getValue(thisRef, property) }
                    .also { cachedValue = it }
            }
        }
    }
}
