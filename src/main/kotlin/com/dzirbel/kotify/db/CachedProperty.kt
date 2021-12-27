package com.dzirbel.kotify.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Returns a [ReadWriteProperty] which delegates to this one, but caches the value to make it available outside of a
 * transaction from [db].
 *
 * This is useful for properties depending on a join where the joined values are unlikely to change, and allows for
 * faster access that does not hit the database in most cases.
 *
 * TODO unit test
 * TODO allow cached value to be null (i.e. if setValue() is called with null)
 */
fun <T, V> ReadWriteProperty<T, V>.cachedOutsideTransaction(db: Database? = null): ReadWriteProperty<T, V> {
    val delegate = this
    return object : ReadWriteProperty<T, V> {
        private var cachedValue: V? = null

        override fun getValue(thisRef: T, property: KProperty<*>): V {
            val inTransaction = db.transactionManager.currentOrNull() != null
            return if (inTransaction) {
                delegate.getValue(thisRef, property)
                    .also { cachedValue = it }
            } else {
                cachedValue?.let { return it }
                transaction(db) { delegate.getValue(thisRef, property) }
                    .also { cachedValue = it }
            }
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            cachedValue = value
            return delegate.setValue(thisRef, property, value)
        }
    }
}
