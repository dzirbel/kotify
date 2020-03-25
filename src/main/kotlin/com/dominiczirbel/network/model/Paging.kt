package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#paging-object
 * https://developer.spotify.com/documentation/web-api/reference-beta/#object-pagingobject
 */
data class Paging<T> (
    /** A link to the Web API endpoint returning the full result of the request. */
    val href: String,

    /** The requested data. */
    val items: List<T>,

    /** The maximum number of items in the response (as set in the query or by default). */
    val limit: Int,

    /** URL to the next page of items. ( null if none) */
    val next: String,

    /** The offset of the items returned (as set in the query or by default). */
    val offset: Int,

    /** URL to the previous page of items. ( null if none) */
    val previous: String,

    /** The maximum number of items available to return. */
    val total: Int
)
