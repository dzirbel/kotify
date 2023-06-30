package com.dzirbel.kotify.network.model

import com.dzirbel.kotify.network.Spotify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

/**
 * An abstract wrapper around [Paging] and [CursorPaging] for convenience to provide common paging functions.
 */
abstract class Pageable<T> {
    abstract val items: List<T>
    abstract val next: String?
}

/**
 * Creates a cold [Flow] that produces values from this [Pageable], first iterating through [Pageable.items] and then
 * continuing recursively though [Pageable]s provided by [fetchNext] with the current [Pageable.next] URL.
 *
 * Note that the type of [Pageable] produced by [fetchNext] must be explicitly specified as [P] so that it can be
 * reified for deserialization by [Spotify.get]; this function is an extension function allow specifying [P] implicitly
 * by the receiver.
 */
inline fun <T, reified P : Pageable<T>> P.asFlow(
    noinline fetchNext: suspend (String) -> P? = { Spotify.get(it) },
): Flow<T> {
    return flow {
        var paging: Pageable<T>? = this@asFlow

        while (paging != null) {
            paging.items.forEach { emit(it) }
            paging = paging.next?.let { fetchNext(it) }
        }
    }
}

@Serializable
data class Paging<T>(
    /** The requested data. */
    override val items: List<T>,

    /** URL to the next page of items. (null if none) */
    override val next: String? = null,

    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** The offset of the items returned (as set in the query or by default). */
    val offset: Int,

    /** URL to the previous page of items. (null if none) */
    val previous: String? = null,

    /** The maximum number of items available to return. */
    val total: Int,
) : Pageable<T>()

@Serializable
data class CursorPaging<T>(
    /** The requested data. */
    override val items: List<T>,

    /** URL to the next page of items. (null if none) */
    override val next: String? = null,

    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** The cursors used to find the next set of items. */
    val cursors: Cursor,

    /** The total number of items available to return. */
    val total: Int,
) : Pageable<T>()

@Serializable
data class Cursor(
    /** The cursor to use as key to find the next page of items. */
    val after: String? = null,
)
