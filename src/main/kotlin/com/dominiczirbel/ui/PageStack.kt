package com.dominiczirbel.ui

/**
 * A marker interface for pages in a [PageStack].
 */
interface Page

/**
 * A simple, immutable stack of [Page]s that can be navigated between.
 *
 * A [PageStack] consists of an ordered list of [pages] and the [currentIndex] of the currently visible pages. Pages
 * before the [currentIndex] are on the "back-stack" and pages after the [currentIndex] are on the "forward-stack". This
 * allows the user to navigate up the stack to a previous page, then forward again.
 *
 * A [PageStack] may be empty, in which case the [currentIndex] should be -1.
 */
class PageStack private constructor(
    val pages: List<Page>,
    val currentIndex: Int
) {
    constructor(page: Page) : this(pages = listOf(page), currentIndex = 0)

    init {
        require(currentIndex in pages.indices || (currentIndex == -1 && pages.isEmpty()))
    }

    /**
     * The currently visible [Page] on the stack, or null if the stack is empty.
     */
    val current: Page?
        get() = pages.getOrNull(currentIndex)

    /**
     * The [Page] immediately before the [current] page on the stack, or null if there is no such page (the back-stack
     * is empty).
     */
    val previous: Page?
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
    val next: Page?
        get() = pages.getOrNull(currentIndex + 1)

    /**
     * Determines whether there are any pages on the forward stack.
     */
    val hasNext: Boolean
        get() = currentIndex < pages.lastIndex

    /**
     * Returns a copy of this [PageStack] which represents the stack navigated to the [previous] page.
     *
     * Throws an [IllegalStateException] if there is no previous page in the back-stack.
     */
    fun toPrevious(): PageStack {
        check(hasPrevious)
        return PageStack(pages = pages, currentIndex = currentIndex - 1)
    }

    /**
     * Returns a copy of this [PageStack] which represents the stack navigated to the [next] page.
     *
     * Throws an [IllegalStateException] if there is no next page in the forward-stack.
     */
    fun toNext(): PageStack {
        check(hasNext)
        return PageStack(pages = pages, currentIndex = currentIndex + 1)
    }

    /**
     * Returns a copy of this [PageStack] having navigated to the given [page].
     *
     * This means the forward-stack will be cleared and replaced with [page], which will now be the [current] page.
     *
     * If [allowDuplicate] is false (the default), [page] will only be added if it is different than the [current] page.
     */
    fun to(page: Page, allowDuplicate: Boolean = false): PageStack {
        if (!allowDuplicate && current == page) return this

        return PageStack(
            pages = pagesMinusForwardStack().plus(page),
            currentIndex = currentIndex + 1
        )
    }

    private fun pagesMinusForwardStack(): List<Page> {
        // a, b, c, d, e
        //       ^- currentIndex = 2
        //
        // a, b, c <- take(3)
        return pages.take(currentIndex + 1)
    }
}
