package com.dzirbel.kotify.ui.framework

/**
 * Simple wrapper around a potentially loaded view model of type [T].
 */
sealed class RemoteState<T> {
    abstract val viewModel: T?

    /**
     * State when the [viewModel] has been successfully loaded.
     */
    data class Loaded<T>(override val viewModel: T) : RemoteState<T>()

    /**
     * State when the view model is still being loaded from the remote source.
     */
    class Loading<T> : RemoteState<T>() {
        override val viewModel: T?
            get() = null
    }

    /**
     * State when the view model could not be found in the remote source.
     */
    class NotFound<T> : RemoteState<T>() {
        override val viewModel: T?
            get() = null
    }
}
