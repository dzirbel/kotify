package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isEmpty
import com.dzirbel.kotify.network.TestSpotifyInterceptor.intercepts
import com.dzirbel.kotify.util.containsExactlyElementsOfInAnyOrder

/**
 * A [Spotify.RequestInterceptor] which intercepts all requests and returns the values provided by [intercepts].
 *
 * This is for use in unit tests which want to avoid making real network calls, and is applied automatically to the
 * [Spotify.Configuration]. It is a workaround for mocking [Spotify] methods to retain purity and performance in tests,
 * but is less convenient in that the exact network call paths/methods must be provided and there aren't (yet) any ways
 * to verify that the expected parameters are supplied for a particular call.
 */
object TestSpotifyInterceptor : Spotify.RequestInterceptor {
    private val interceptedCalls = mutableListOf<String>()

    // { method -> { path -> [responses] } }
    private val intercepts: MutableMap<String, MutableMap<String, MutableList<Any?>>> = mutableMapOf()

    /**
     * Adds an [responses] as intercept(s) for GET requests on the given [path], overriding any existing intercepts for
     * [path].
     *
     * Each of the [responses] will be returned for successive calls until none remain.
     *
     * This is an convenience alternative of [intercept] to allow providing the method to be optional without
     * interfering with the vararg parameters.
     */
    fun <T> intercept(path: String, vararg responses: T) {
        intercept(path = path, method = "GET", responses = responses)
    }

    /**
     * Adds an [responses] as intercept(s) for requests of the given [method] on the given [path], overriding any
     * existing intercepts for [method] and [path].
     *
     * Each of the [responses] will be returned for successive calls until none remain.
     */
    fun <T> intercept(path: String, method: String, vararg responses: T) {
        intercepts.getOrPut(method) { mutableMapOf() }[path] = responses.toMutableList()
    }

    /**
     * Asserts that all the responses provided by [intercept] have been consumed by calls to [Spotify]. This is useful
     * to verify that a particular test made the expected set of network calls (without needing to explicitly repeat
     * them as for [verifyInterceptedCalls]).
     */
    fun verifyAllIntercepted() {
        val unusedIntercepts = intercepts
            .flatMap { it.value.entries }
            .filter { it.value.isNotEmpty() }
        assertThat(unusedIntercepts).isEmpty()
    }

    /**
     * Asserts that exactly the given [calls] (each a request path on the Spotify API) have been made.
     */
    fun verifyInterceptedCalls(calls: Collection<String>) {
        assertThat(interceptedCalls).containsExactlyElementsOfInAnyOrder(calls)
    }

    /**
     * Clears the set of registered intercept responses and past calls, for cleanup between tests.
     */
    fun reset() {
        interceptedCalls.clear()
        intercepts.clear()
        intercepts.clear()
    }

    override fun interceptFor(method: String, path: String): Any? {
        interceptedCalls.add(path)
        if (intercepts[method]?.contains(path) == true) {
            val intercepts = intercepts[method]?.get(path)
            if (intercepts.isNullOrEmpty()) {
                error("ran out of intercepts for $method $path")
            }

            return intercepts.removeFirst()
        }

        error("no intercept registered for $method $path")
    }
}
