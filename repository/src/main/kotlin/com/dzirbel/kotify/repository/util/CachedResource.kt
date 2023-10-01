package com.dzirbel.kotify.repository.util

import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.CacheStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
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
    private val getFromCache: suspend () -> Pair<T, Instant>?,
    private val getFromRemote: suspend () -> Pair<T, Instant>?,
    private val cacheStrategy: CacheStrategy<Pair<T, Instant>> = CacheStrategy.AlwaysValid(),
) {
    private val _flow: MutableStateFlow<CacheState<T>?> = MutableStateFlow(null)

    private val init = AtomicBoolean(false)
    private val initFinished = AtomicBoolean(false)
    private val ensuredLoaded = AtomicBoolean(false)

    private var remoteJob: Job? = null
    private var cacheJob: Job? = null

    private var validity: CacheStrategy.CacheValidity? = null

    /**
     * A [StateFlow] reflecting the current value of the [CachedResource]; initially null.
     */
    val flow: StateFlow<CacheState<T>?>
        get() = _flow

    /**
     * Idempotently and asynchronously initializes this [CachedResource] from the local cache.
     */
    fun initFromCache() {
        if (!init.getAndSet(true)) {
            _flow.value = CacheState.Refreshing.of(_flow.value)
            cacheJob = scope.launch {
                _flow.value = try {
                    getFromCache()?.let { (value, cacheTime) ->
                        validity = cacheStrategy.validity(value to cacheTime)
                        if (validity?.canBeUsed == true) {
                            CacheState.Loaded(value, cacheTime)
                        } else {
                            null
                        }
                    }
                } catch (_: CancellationException) {
                    null
                } catch (throwable: Throwable) {
                    CacheState.Error(throwable)
                }

                initFinished.set(true)

                if (ensuredLoaded.get() && validity?.shouldBeRefreshed != false) {
                    launchRemote()
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

            if (initFinished.get() && validity?.shouldBeRefreshed != false) {
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
    }

    /**
     * Updates the value of the [flow] via the given [transform] mutation.
     */
    fun map(transform: (T) -> T) {
        _flow.value = _flow.value?.map(transform)
    }

    private fun launchRemote() {
        val previousValue = _flow.value
        _flow.value = CacheState.Refreshing.of(previousValue)
        remoteJob?.cancel()
        remoteJob = scope.launch {
            _flow.value = try {
                getFromRemote()?.let { CacheState.Loaded(it.first, it.second) }
                    ?: CacheState.NotFound()
            } catch (_: CancellationException) {
                previousValue
            } catch (throwable: Throwable) {
                CacheState.Error(throwable)
            }
        }
    }
}
