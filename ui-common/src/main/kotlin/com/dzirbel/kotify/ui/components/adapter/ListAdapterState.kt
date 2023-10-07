package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.coroutines.Computation
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch

/**
 * Convenience function to create a [remember]ed [ListAdapterState] with the given parameters and a [CoroutineScope]
 * local to this point in the composition.
 */
@Composable
fun <E> rememberListAdapterState(
    key: Any? = null,
    scope: CoroutineScope = rememberCoroutineScope { Dispatchers.Computation },
    defaultSort: SortableProperty<E>? = null,
    defaultFilter: ((E) -> Boolean)? = null,
    source: (CoroutineScope) -> StateFlow<List<E>?>,
): ListAdapterState<E> {
    val sourceFlow = remember(key) { source(scope) }
    val initialValue = remember(key) { sourceFlow.value }
    return remember(key) {
        ListAdapterState(
            initialValue = initialValue,
            scope = scope,
            defaultSort = defaultSort,
            defaultFilter = defaultFilter,
        )
    }
        .also { listAdapterState ->
            // collect in a LaunchedEffect to ensure the collected value does not attempt to mutate before the snapshot
            // is applied
            LaunchedEffect(key) {
                sourceFlow
                    // skip initial value to avoid immediate mutation
                    .dropWhile { it == initialValue }
                    .collect { elements ->
                        listAdapterState.mutate { withElements(elements) }
                    }
            }
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
    initialValue: List<E>?,
    defaultSort: SortableProperty<E>? = null,
    defaultFilter: ((E) -> Boolean)? = null,
) : State<ListAdapter<E>> {
    private val state = mutableStateOf(
        ListAdapter.of(
            elements = initialValue,
            defaultSort = defaultSort,
            defaultFilter = defaultFilter,
        ),
    )

    override val value: ListAdapter<E>
        get() = state.value

    /**
     * Asynchronously updates the [ListAdapter] to the result of [block] on the current [value].
     */
    fun mutate(block: ListAdapter<E>.() -> ListAdapter<E>) {
        scope.launch { state.mutate(block) }
    }

    /**
     * Convenience wrapper around [mutate] which applies [ListAdapter.withSort].
     */
    fun withSort(sorts: PersistentList<Sort<E>>?) {
        mutate { withSort(sorts) }
    }

    /**
     * Convenience wrapper around [mutate] which applies [ListAdapter.withDivider].
     */
    fun withDivider(divider: Divider<E>?) {
        mutate { withDivider(divider) }
    }

    /**
     * Convenience wrapper around [mutate] which applies [ListAdapter.withFilter].
     */
    fun withFilter(filter: ((E) -> Boolean)?) {
        mutate { withFilter(filter = filter) }
    }
}
