package com.dzirbel.kotify.ui.framework

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Represents a page in a [PageStack] and how it is rendered.
 */
@Stable
abstract class Page<T> {
    /**
     * The [MutableTransitionState] which contains the animation state of the page name and highlighted background for
     * the navigation title.
     *
     * This can be toggled by pages e.g. when the user scrolls past a header section, to retain the page title on the
     * screen. When true the title will be shown in the navigation header, when false it will not be. Each [Page]
     * retains its own state so that navigating in the page stack does not start a new animation for each new page -
     * transitions between pages should be instant.
     */
    val navigationTitleState = MutableTransitionState(false)

    /**
     * Binds this page to the composition, optionally rendering its content if [visible] is true; returning some
     * stateful data [T] which can be used to derive the [titleFor] the page.
     *
     * This function is called by the [PageStack] container for all pages on the stack, and serves two purposes. For
     * pages which are not currently [visible], bind allows the page to persist data which is attached to the
     * composition, but should not use it to render visible elements. For the page which is [visible], the page can
     * both attach data to the composition and render its contents in this call. Ideally these functions might be
     * separated, but given that the data bound to the composition is used to drive the rendered content it is
     * convenient to keep them together and allow the page to determine how they interact.
     */
    @Composable
    abstract fun BoxScope.bind(visible: Boolean): T

    /**
     * Determines the user-visible title for this page, which may optionally use [data] of type [T] returned by the most
     * recent call to [bind] for the page.
     *
     * Use of [data] allows pages to construct a title based on data they load, e.g. the page displaying an artist would
     * need to load the artist's name.
     */
    abstract fun titleFor(data: T): String?
}
