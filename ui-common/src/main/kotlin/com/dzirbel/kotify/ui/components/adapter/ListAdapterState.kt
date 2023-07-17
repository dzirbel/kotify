package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Convenience function to create a [remember]ed [ListAdapterState] with the given parameters and a [CoroutineScope]
 * local to this point in the composition.
 */
@Composable
fun <E> rememberListAdapterState(
    source: (CoroutineScope) -> StateFlow<List<E>?>,
    key: Any? = null,
    defaultSort: SortableProperty<E>? = null,
    defaultFilter: ((E) -> Boolean)? = null,
): ListAdapterState<E> {
    val scope = rememberCoroutineScope()
    return remember(key) {
        ListAdapterState(
            source = source(scope),
            scope = scope,
            defaultSort = defaultSort,
            defaultFilter = defaultFilter,
        )
    }
}

/**
 * A [State] wrapper around a [ListAdapter] which applies operations sequentially in the given [scope].
 *
 * This avoids race conditions when applying changes (e.g. new elements or changing the sort order) to the [ListAdapter]
 * in different places.
 *
 * @param scope [CoroutineScope] in which [mutate] is applied and [source] is collected
 * @param source an optional [Flow] whose emissions are applied as elements of the [ListAdapter]
 * @param defaultSort the initial sort order for the [ListAdapter]
 * @param defaultFilter the initial filter for the [ListAdapter]
 */
@Suppress("OutdatedDocumentation") // detekt false positive (?)
@Stable
class ListAdapterState<E>(
    private val scope: CoroutineScope,
    source: StateFlow<List<E>?>,
    defaultSort: SortableProperty<E>? = null,
    defaultFilter: ((E) -> Boolean)? = null,
) : State<ListAdapter<E>> {
    private val state = mutableStateOf(
        ListAdapter.of(
            elements = source.value,
            defaultSort = defaultSort,
            defaultFilter = defaultFilter,
        ),
    )

    override val value: ListAdapter<E>
        get() = state.value

    init {
        scope.launch {
            source.collect { elements ->
                mutate { withElements(elements) }
            }
        }
    }

    fun mutate(block: ListAdapter<E>.() -> ListAdapter<E>) {
        scope.launch { state.mutate(block) }
    }
}
