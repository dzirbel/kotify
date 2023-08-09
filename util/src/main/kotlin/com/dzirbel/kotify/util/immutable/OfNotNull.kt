package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

fun <T : Any> persistentListOfNotNull(element: T?): PersistentList<T> {
    return if (element != null) persistentListOf(element) else persistentListOf()
}

fun <T : Any> persistentListOfNotNull(vararg elements: T?): PersistentList<T> {
    return listOfNotNull(*elements).toPersistentList()
}
