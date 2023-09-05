package com.dzirbel.kotify.repository.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds logic around managing a single resource of type [T] which may be loaded from either from a local source via
 * [getFromCache] or remote source of truth via [getFromRemote].
 *
 * In particular, this abstraction exists to consolidate logic that can handle a few tricky cases:
 * - the resource is loaded async from a cache, where it may or may not be present, on application start
 * - at some point during application lifecycle, a request is made to ensure there is a cached value (i.e. either from
 *   the cache or the remote data source if not)
 * - this request might come before the initial load from cache is complete (in which case a load from remote should be
 *   done after the cache load if and only if the resource was not present in the cache)
 * - the cached resource can be forcefully refreshed from the remote or invalidated
 */
class CachedResource<T : Any>(
    private val scope: CoroutineScope,
    private val getFromCache: suspend () -> T?,
    private val getFromRemote: suspend () -> T?,
) {
    private val _flow: MutableStateFlow<T?> = MutableStateFlow(null)
    private val _refreshingFlow = MutableStateFlow(false)

    private val init = AtomicBoolean(false)
    private val initFinished = AtomicBoolean(false)
    private val ensuredLoaded = AtomicBoolean(false)

    private var remoteJob: Job? = null
    private var cacheJob: Job? = null

    /**
     * A [StateFlow] reflecting the current value of the [CachedResource]; initially null.
     */
    val flow: StateFlow<T?>
        get() = _flow

    /**
     * A [StateFlow] reflecting whether the [CachedResource] is currently being refreshed (either from the cache or the
     * remote source of truth).
     */
    val refreshingFlow: StateFlow<Boolean>
        get() = _refreshingFlow

    /**
     * Idempotently and asynchronously initializes this [CachedResource] from the local cache.
     */
    fun initFromCache() {
        if (!init.getAndSet(true)) {
            _refreshingFlow.value = true
            cacheJob = scope.launch {
                val cached = getFromCache()
                ensureActive() // do not apply cached value if cancelled
                _flow.value = cached
                initFinished.set(true)

                if (ensuredLoaded.get() && cached == null) {
                    launchRemote()
                } else {
                    _refreshingFlow.value = false
                }
            }
        }
    }

    /**
     * Idempotently and asynchronously ensures the resource is loaded; first attempting to load it from the cache and
     * then from the remote source of truth.
     */
    fun ensureLoaded() {
        if (!ensuredLoaded.getAndSet(true)) {
            initFromCache()

            if (initFinished.get() && _flow.value == null) {
                launchRemote()
            }
        }
    }

    /**
     * Fetches a new value from the remote source of truth.
     */
    fun refreshFromRemote() {
        cacheJob?.cancel() // ensure any new loads from cache do not overwrite the new value from the remote
        ensuredLoaded.set(true)
        launchRemote()
    }

    /**
     * Invalidates the current state of the [CachedResource], forcing it to be initialized or loaded again.
     */
    fun invalidate() {
        cacheJob?.cancel()
        remoteJob?.cancel()

        init.set(false)
        initFinished.set(false)
        ensuredLoaded.set(false)

        _flow.value = null
        _refreshingFlow.value = false
    }

    /**
     * Updates the value of the [flow] via the given [update] mutation.
     */
    fun update(update: (T) -> T) {
        _flow.value?.let { value -> _flow.value = update(value) }
    }

    private fun launchRemote() {
        _refreshingFlow.value = true
        remoteJob?.cancel()
        remoteJob = scope.launch {
            val remote = getFromRemote()
            ensureActive() // do not apply new remote value if cancelled
            _flow.value = remote
            _refreshingFlow.value = false
        }
    }
}
