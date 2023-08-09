package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

fun <E> ImmutableList<E>?.orEmpty(): ImmutableList<E> {
    return this ?: persistentListOf()
}

fun <E> PersistentList<E>?.orEmpty(): PersistentList<E> {
    return this ?: persistentListOf()
}
