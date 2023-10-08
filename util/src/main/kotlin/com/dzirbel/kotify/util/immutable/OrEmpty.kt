package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

fun <E> PersistentList<E>?.orEmpty(): PersistentList<E> {
    return this ?: persistentListOf()
}
