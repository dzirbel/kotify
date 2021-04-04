package com.dominiczirbel.cache

import java.io.File
import kotlin.time.Duration

// TODO add batched get and put events
sealed class CacheEvent {
    abstract val cache: Cache

    data class Load(override val cache: Cache, val duration: Duration, val file: File) : CacheEvent()
    data class Save(override val cache: Cache, val duration: Duration, val file: File) : CacheEvent()
    data class Dump(override val cache: Cache) : CacheEvent()
    data class Clear(override val cache: Cache) : CacheEvent()
    data class Hit(override val cache: Cache, val id: String, val value: CacheObject) : CacheEvent()
    data class Miss(override val cache: Cache, val id: String) : CacheEvent()
    data class Update(override val cache: Cache, val id: String, val previous: CacheObject?, val new: CacheObject) :
        CacheEvent()

    data class Invalidate(override val cache: Cache, val id: String, val value: CacheObject) : CacheEvent()
}
