package com.dzirbel.kotify.util.coroutines

import kotlinx.coroutines.selects.SelectClause2
import kotlinx.coroutines.sync.Mutex

/**
 * A [Mutex] which locks multiple [Mutex]es in order.
 *
 * WARNING: use of [MergedMutex] is dangerous and can easily result in deadlocks if not used carefully. In particular,
 * if two [MergedMutex]es have multiple [Mutex]es in common, locking may deadlock if the lock order (i.e. the order in
 * [mutexes]) is different. For this reason, a single shared [MergedMutex] instance should be used to lock a group of
 * resources whenever possible.
 */
class MergedMutex(private val mutexes: Iterable<Mutex>) : Mutex {
    private val parentMutex = Mutex()

    override val isLocked: Boolean
        get() = parentMutex.isLocked

    @Suppress("OVERRIDE_DEPRECATION")
    override val onLock: SelectClause2<Any?, Mutex>
        get() = error("not implemented")

    override fun holdsLock(owner: Any): Boolean {
        return parentMutex.holdsLock(owner)
    }

    override suspend fun lock(owner: Any?) {
        parentMutex.lock(owner)
        for (mutex in mutexes) mutex.lock(owner)
    }

    override fun tryLock(owner: Any?): Boolean {
        if (!parentMutex.tryLock(owner)) return false

        val locked = mutableListOf(parentMutex)

        for (mutex in mutexes) {
            if (!mutex.tryLock(owner)) {
                for (lockedMutex in locked) lockedMutex.unlock(owner)
                return false
            }

            // add at index 0 so iteration is in reverse locking order
            locked.add(0, mutex)
        }

        return true
    }

    override fun unlock(owner: Any?) {
        for (mutex in mutexes.reversed()) mutex.unlock(owner)
        parentMutex.unlock(owner)
    }
}
