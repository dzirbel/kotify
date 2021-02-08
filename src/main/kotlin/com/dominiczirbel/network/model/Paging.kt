package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Paging<T>(
    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The requested data. */
    val items: List<T>,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** URL to the next page of items. (null if none) */
    val next: String? = null,

    /** The offset of the items returned (as set in the query or by default). */
    val offset: Int,

    /** URL to the previous page of items. (null if none) */
    val previous: String? = null,

    /** The maximum number of items available to return. */
    val total: Int
)

@Serializable
data class CursorPaging<T>(
    val href: String,
    val items: List<T>,
    val limit: Int,
    val next: String? = null,
    val cursors: Cursor,
    val total: Int
)

@Serializable
data class Cursor(val after: String? = null)
