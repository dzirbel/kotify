package com.dzirbel.kotify.repository.player

/**
 * Represents the state of a player element that can be toggled between states, such as the play button toggling between
 * playing and paused.
 *
 * [Set] corresponds to a "stable" state which is not being changed and [TogglingTo] to the state where the state is in
 * the process of being updated to the new [value].
 */
sealed interface ToggleableState<T> {
    /**
     * The value to be displayed for this state, either the current [Set.value] or the upcoming [TogglingTo.value].
     */
    val value: T

    data class Set<T>(override val value: T) : ToggleableState<T>
    data class TogglingTo<T>(override val value: T) : ToggleableState<T>
}
