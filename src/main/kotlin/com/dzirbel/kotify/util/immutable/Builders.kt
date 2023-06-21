package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

fun <T : Any> persistentListOfNotNull(element: T?): PersistentList<T> {
    return if (element != null) persistentListOf(element) else persistentListOf()
}

fun <T : Any> persistentListOfNotNull(vararg elements: T?): PersistentList<T> {
    return persistentListOf<T>().mutate { mutableList ->
        for (element in elements) {
            if (element != null) {
                mutableList.add(element)
            }
        }
    }
}
