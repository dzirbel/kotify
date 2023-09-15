package com.dzirbel.kotify.repository

import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.error
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.log.warn
import com.dzirbel.kotify.util.CurrentTime
import kotlin.time.Duration
import kotlin.time.TimeMark

/**
 * A stateful wrapper class which tracks metadata for requests to [Repository]s and logs them to [log].
 */
internal class RequestLog(
    private val log: MutableLog<Repository.LogData>,
    private val start: TimeMark = CurrentTime.mark,
    private var timeInDb: Duration? = null,
    private var timeInRemote: Duration? = null,
) {
    private val duration: Duration
        get() = start.elapsedNow()

    fun addDbTime(duration: Duration): RequestLog {
        timeInDb = timeInDb?.let { it + duration } ?: duration
        return this
    }

    fun addRemoteTime(duration: Duration): RequestLog {
        timeInRemote = timeInRemote?.let { it + duration } ?: duration
        return this
    }

    fun info(title: String, source: DataSource) {
        log.info(title = title, data = toLogData(source), duration = duration)
    }

    fun success(title: String, source: DataSource) {
        log.success(title = title, data = toLogData(source), duration = duration)
    }

    fun warn(title: String, source: DataSource, throwable: Throwable? = null) {
        if (throwable == null) {
            log.warn(title = title, data = toLogData(source), duration = duration)
        } else {
            log.warn(throwable = throwable, title = title, data = toLogData(source), duration = duration)
        }
    }

    fun error(title: String, source: DataSource, throwable: Throwable? = null) {
        if (throwable == null) {
            log.error(title = title, data = toLogData(source), duration = duration)
        } else {
            log.error(throwable = throwable, title = title, data = toLogData(source), duration = duration)
        }
    }

    private fun toLogData(source: DataSource): Repository.LogData {
        return Repository.LogData(source = source, timeInRemote = timeInRemote, timeInDb = timeInDb)
    }
}
