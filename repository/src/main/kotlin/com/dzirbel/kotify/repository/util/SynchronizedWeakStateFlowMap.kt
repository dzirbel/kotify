package com.dzirbel.kotify.repository.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.util.Collections

/**
 * A wrapper around a [MutableMap] with keys [K] and values [MutableStateFlow] of [V] which is both synchronized
 * (supports multi-threaded read and write access) and keeps only weak references to the [MutableStateFlow] values.
 *
 * This combination is important for typical repository operations, which often want to track the state of a variety of
 * objects (as [MutableStateFlow]s) by their IDs (as keys [K]). Synchronization is important since repositories are
 * multi-threaded. Weak references avoid the memory footprint of the repository from growing without bound by
 * restricting the [StateFlow]s being tracked to only those with external references, e.g. actively used by the UI.
 *
 * This class exposes a minimal and convenient API for repository operations rather than directly implementing [Map].
 * In particular, it hides the implementation details of wrapping values in [WeakReference] and [MutableStateFlow] and
 * exposes an API that interacts with the values [V] directly when possible.
 */
class SynchronizedWeakStateFlowMap<K : Any, V : Any> {
    // TODO tends to accumulate empty WeakReference: this could be improved by creating a standalone WeakValueHashMap
    //  with logic to sweep GC'd references from the map, but while some reference Java implementations exist, this is
    //  non-trivial
    // note: WeakHashMap uses weak keys, not weak values, so it is not helpful here
    private val stateFlowMap: MutableMap<K, WeakReference<MutableStateFlow<V?>>> =
        Collections.synchronizedMap(mutableMapOf())

    /**
     * Retrieves the current value of the [StateFlow] for the given [key], or null if either it does not exist (has not
     * been created in the map or was garbage collected) or the value of the [StateFlow] is null.
     */
    fun getValue(key: K): V? {
        return stateFlowMap[key]?.get()?.value
    }

    /**
     * Gets the [StateFlow] tracking the value associated with the given [key], creating one with a default from
     * [defaultValue] if it is not present in the map (or has been garbage collected).
     *
     * If a new [StateFlow] was created, [onCreate] is invoked with the default value used.
     */
    fun getOrCreateStateFlow(
        key: K,
        defaultValue: () -> V? = { null },
        onExisting: (V?) -> Unit = {},
        onCreate: (V?) -> Unit = {},
    ): StateFlow<V?> {
        var created = false
        var default: V? = null
        return synchronized(stateFlowMap) {
            stateFlowMap[key]?.get()
                ?: run {
                    default = defaultValue()
                    MutableStateFlow(default)
                        .also { stateFlowMap[key] = WeakReference(it) }
                        .also { created = true }
                }
        }
            .also { stateFlow ->
                if (created) {
                    onCreate(default)
                } else {
                    onExisting(stateFlow.value)
                }
            }
    }

    /**
     * Gets a batch of [StateFlow]s tracking the values associated with the given [keys] in a single atomic operation,
     * creating them with the defaults from [defaultValue] if not present in the map (or if they have been garbage
     * collected).
     *
     * The returned list has the same size and is in the same order as [keys].
     *
     * If any new [StateFlow]s are created, [onCreate] is called with a map from the subset of keys for which they have
     * been created to the default values used for each.
     */
    fun getOrCreateStateFlows(
        keys: Iterable<K>,
        defaultValue: (K) -> V? = { null },
        onExisting: (Int) -> Unit = {},
        onCreate: (Map<K, V?>) -> Unit = {},
    ): List<StateFlow<V?>> {
        val created = mutableMapOf<K, V?>()
        return synchronized(stateFlowMap) {
            keys.map { key ->
                stateFlowMap[key]?.get()
                    ?: run {
                        val default = defaultValue(key)
                        MutableStateFlow(default)
                            .also { stateFlowMap[key] = WeakReference(it) }
                            .also { created[key] = default }
                    }
            }
        }
            .also {
                if (created.isNotEmpty()) {
                    onCreate(created)
                }

                val numExisting = keys.count() - created.size
                if (numExisting > 0) {
                    onExisting(numExisting)
                }
            }
    }

    /**
     * Sets the value of the [StateFlow] associated with the given [key] to the given [value], if present in the map
     * (has been created and has not been garbage collected).
     *
     * If the [StateFlow] is not present in the map or has been garbage collected, this is a no-op.
     */
    fun updateValue(key: K, value: V?) {
        stateFlowMap[key]?.get()?.value = value
    }

    /**
     * Sets the value of the [StateFlow] associated with the given [key] to the value produced by invoking [valueMapper]
     * with the current [StateFlow] value, if present in the map (has been created and has not been garbage collected).
     *
     * If the [StateFlow] is not present in the map or has been garbage collected, this is a no-op.
     */
    fun updateValue(key: K, valueMapper: (V?) -> V?) {
        val flow = stateFlowMap[key]?.get()
        if (flow != null) {
            flow.value = valueMapper(flow.value)
        }
    }

    /**
     * Invokes [computation] on all the keys of the map which have active [StateFlow] values and updates their values to
     * the result.
     */
    fun computeAll(computation: (K) -> V?) {
        synchronized(stateFlowMap) {
            for ((id, weakReference) in stateFlowMap) {
                val flow = weakReference.get()
                if (flow != null) {
                    flow.value = computation(id)
                }
            }
        }
    }

    /**
     * Clears all [StateFlow]s held in the map.
     */
    fun clear() {
        stateFlowMap.clear()
    }
}
