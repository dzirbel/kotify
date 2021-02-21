package com.dominiczirbel.network.model

import com.dominiczirbel.network.Spotify
import kotlinx.serialization.Serializable

/**
 * An abstract wrapper around [Paging] and [CursorPaging] for convenience to provide common paging functions.
 */
abstract class Pageable<T> {
    abstract val items: List<T>
    abstract val hasNext: Boolean

    /**
     * Fetches all the items in this [Pageable], i.e. calls [fetchNext] on each [Pageable] until one where [hasNext] is
     * false is reached, and accumulates the [items].
     *
     * Generally, [Paging.fetchAll] or [CursorPaging.fetchAll] should be used instead for convenience.
     *
     * This fetches all values immediately, rather than on-demand, and so may not be appropriate for all use cases.
     *
     * TODO implement a lazy fetch (likely via an iterator())
     */
    inline fun <reified S : Pageable<out T>> fetchAll(fetchNext: (S) -> S?): List<T> {
        val all = mutableListOf<T>()

        var current: S? = this as S
        while (current != null) {
            all.addAll(current.items)
            current = current.takeIf { it.hasNext }?.let { fetchNext(it) }
        }

        return all
    }
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-pagingobject
 */
@Serializable
data class Paging<T>(
    /** The requested data. */
    override val items: List<T>,

    /** URL to the next page of items. (null if none) */
    val next: String? = null,

    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** The offset of the items returned (as set in the query or by default). */
    val offset: Int,

    /** URL to the previous page of items. (null if none) */
    val previous: String? = null,

    /** The maximum number of items available to return. */
    val total: Int
) : Pageable<T>() {
    override val hasNext: Boolean
        get() = next != null

    /**
     * Fetches all the items in this [Paging], i.e. its [items] and the [items] in all the [next] [Paging] objects.
     */
    suspend inline fun <reified S : T> fetchAll(): List<T> {
        return fetchAll<Paging<S>> { paging -> paging.next?.let { Spotify.get(it) } }
    }
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-cursorpagingobject
 */
@Serializable
data class CursorPaging<T>(
    /** The requested data. */
    override val items: List<T>,

    /** URL to the next page of items. (null if none) */
    val next: String? = null,

    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** The cursors used to find the next set of items. */
    val cursors: Cursor,

    /** The total number of items available to return. */
    val total: Int
) : Pageable<T>() {
    override val hasNext: Boolean
        get() = next != null

    /**
     * Fetches all the items in this [CursorPaging], i.e. its [items] and the [items] in all the [next] [CursorPaging]
     * objects.
     */
    suspend inline fun <reified S : T> fetchAll(): List<T> {
        return fetchAll<CursorPaging<S>> { paging -> paging.next?.let { Spotify.get(it) } }
    }
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-cursorobject
 */
@Serializable
data class Cursor(
    /** The cursor to use as key to find the next page of items. */
    val after: String? = null
)
