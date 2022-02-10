package com.dzirbel.kotify.ui.framework

import kotlinx.coroutines.CoroutineScope

abstract class RemoteStatePresenter<ViewModel, Event : Any>(
    scope: CoroutineScope,
    key: Any? = null,
    eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.LATEST_BY_CLASS,
    startingEvents: List<Event>? = null,
) : Presenter<RemoteState<ViewModel>, Event>(
    scope = scope,
    initialState = RemoteState.Loading(),
    key = key,
    eventMergeStrategy = eventMergeStrategy,
    startingEvents = startingEvents,
) {
    protected fun mutateLoadedState(transform: (ViewModel) -> ViewModel) {
        mutateState { state ->
            (state as? RemoteState.Loaded)
                ?.viewModel
                ?.let(transform)
                ?.let { RemoteState.Loaded(it) }
        }
    }

    protected fun initializeLoadedState(init: (ViewModel?) -> ViewModel) {
        mutateState { state ->
            RemoteState.Loaded(init(state.viewModel))
        }
    }
}
