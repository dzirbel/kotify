package com.dzirbel.kotify.ui.framework

/**
 * A simple, immutable stack of [Page]s that can be navigated between.
 *
 * A [PageStack] consists of an ordered list of [pages] and the [currentIndex] of the currently visible pages. Pages
 * before the [currentIndex] are on the "back-stack" and pages after the [currentIndex] are on the "forward-stack". This
 * allows the user to navigate up the stack to a previous page, then forward again.
 *
 * A [PageStack] may be not be empty.
 */
class PageStack private constructor(
    val pages: List<Page<*>>,
    val currentIndex: Int,
) {
    init {
        require(currentIndex in pages.indices) { "index out of bounds: $currentIndex not in ${pages.indices}" }
    }

    /**
     * The currently visible [Page] on the stack.
     */
    val current: Page<*>
        get() = pages[currentIndex]

    /**
     * The [Page] immediately before the [current] page on the stack, or null if there is no such page (the back-stack
     * is empty).
     */
    val previous: Page<*>?
        get() = pages.getOrNull(currentIndex - 1)

    /**
     * Determines whether there are any pages on the back stack.
     */
    val hasPrevious: Boolean
        get() = currentIndex > 0

    /**
     * The [Page] immediately after the [current] page on the stack, or null if there is no such page (the forward-stack
     * is empty).
     */
    val next: Page<*>?
        get() = pages.getOrNull(currentIndex + 1)

    /**
     * Determines whether there are any pages on the forward stack.
     */
    val hasNext: Boolean
        get() = currentIndex < pages.lastIndex

    constructor(page: Page<*>) : this(pages = listOf(page), currentIndex = 0)

    /**
     * Returns a copy of this [PageStack] which represents the stack navigated to the [previous] page.
     *
     * Throws an [IllegalStateException] if there is no previous page in the back-stack.
     */
    fun toPrevious(): PageStack {
        return toIndex(index = currentIndex - 1)
    }

    /**
     * Returns a copy of this [PageStack] which represents the stack navigated to the [next] page.
     *
     * Throws an [IllegalStateException] if there is no next page in the forward-stack.
     */
    fun toNext(): PageStack {
        return toIndex(index = currentIndex + 1)
    }

    /**
     * Returns a copy of this [PageStack] which represents the stack navigated to the page at [index].
     *
     * Throws an [IllegalStateException] if [index] is out of bounds.
     */
    fun toIndex(index: Int): PageStack {
        check(index in pages.indices)
        return PageStack(pages = pages, currentIndex = index)
    }

    /**
     * Returns a copy of this [PageStack] having navigated to the given [page].
     *
     * This means the forward-stack will be cleared and replaced with [page], which will now be the [current] page.
     *
     * If [allowDuplicate] is false (the default), [page] will only be added if it is different than the [current] page.
     */
    fun to(page: Page<*>, allowDuplicate: Boolean = false): PageStack {
        if (!allowDuplicate && current == page) return this

        return PageStack(
            pages = pages.minusForwardStack().plus(page),
            currentIndex = currentIndex + 1,
        )
    }

    private fun <T> List<T>.minusForwardStack(): List<T> {
        // a, b, c, d, e
        //       ^- currentIndex = 2
        //
        // a, b, c <- take(3)
        return take(currentIndex + 1)
    }
}
