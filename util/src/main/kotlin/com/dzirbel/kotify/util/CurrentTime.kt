package com.dzirbel.kotify.util

import java.time.Instant
import java.time.ZoneId
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A wrapper providing access to a system wall-clock as either a timestamp via [CurrentTime.millis] or [Instant] via
 * [CurrentTime.instant].
 *
 * This is preferred over direct calls to [System.currentTimeMillis] et al. to centralize access (thus making it more
 * clear where the system time is being used) and to allow the time to be easily (and without reflection) mocked in
 * tests.
 */
@Suppress("ForbiddenMethodCall") // allow calls to system time
object CurrentTime {
    /**
     * Whether access to the system time is currently enabled.
     *
     * Defaults to false to ensure that tests only access the time when it has been mocked; while running the
     * application this should be set to true immediately on startup.
     */
    var enabled = false

    private var mockedTime: Long? = null
    private const val DEFAULT_MOCKED_TIME = 1_681_977_723_000L

    private var mockedZoneId: ZoneId? = null
    private val DEFAULT_MOCKED_ZONE_ID = ZoneId.of("America/New_York")

    /**
     * The current system time as a timestamp.
     */
    val millis: Long
        get() {
            check(enabled) { "access to system time is disabled" }
            return mockedTime ?: System.currentTimeMillis()
        }

    /**
     * The current system time as an [Instant].
     */
    val instant: Instant
        get() {
            check(enabled) { "access to system time is disabled" }
            return mockedTime?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
        }

    /**
     * The current timezone as a [ZoneId].
     */
    val zoneId: ZoneId
        get() {
            check(enabled) { "access to system time is disabled" }
            return mockedZoneId ?: ZoneId.systemDefault()
        }

    /**
     * Retrieves a [TimeMark] of the current time.
     */
    val mark: ComparableTimeMark
        get() {
            check(enabled) { "access to system time is disabled" }
            return mockedTime?.let { MockedTimeMark(it) } ?: TimeSource.Monotonic.markNow()
        }

    /**
     * Mocks calls to [CurrentTime] within block, returning its result.
     *
     * Should only be used in tests.
     */
    fun <T> mocked(millis: Long = DEFAULT_MOCKED_TIME, zoneId: ZoneId? = DEFAULT_MOCKED_ZONE_ID, block: () -> T): T {
        startMock(millis = millis, zoneId = zoneId)
        return try {
            block()
        } finally {
            closeMock()
        }
    }

    /**
     * Mocks calls to [CurrentTime] until [closeMock] is called.
     *
     * [mocked] is generally preferred for safety, but this can be used in a test setup callback.
     */
    fun startMock(millis: Long = DEFAULT_MOCKED_TIME, zoneId: ZoneId? = DEFAULT_MOCKED_ZONE_ID) {
        check(!enabled) { "system time is already enabled or mocked" }
        enabled = true
        mockedTime = millis
        mockedZoneId = zoneId
    }

    /**
     * Advances the mocked time by [millis].
     */
    fun advanceMock(millis: Long) {
        check(enabled) { "system time is not enabled" }
        mockedTime = requireNotNull(mockedTime) { "system time has not been mocked" }.plus(millis)
    }

    /**
     * Stops mocking calls to [CurrentTime].
     */
    fun closeMock() {
        check(enabled) { "system time was not being mocked" }
        enabled = false
        mockedTime = null
        mockedZoneId = null
    }

    private class MockedTimeMark(private val mockedTime: Long) : ComparableTimeMark {
        override fun elapsedNow(): Duration {
            val currentTime = requireNotNull(CurrentTime.mockedTime) { "comparing mocked TimeMark with unmarked time" }
            return (currentTime - mockedTime).milliseconds
        }

        override fun minus(other: ComparableTimeMark): Duration {
            require(other is MockedTimeMark) { "comparing non-mocked TimeMark $other" }
            return (mockedTime - other.mockedTime).milliseconds
        }

        override fun plus(duration: Duration): ComparableTimeMark {
            return MockedTimeMark(mockedTime + duration.inWholeMilliseconds)
        }

        override fun equals(other: Any?) = other is MockedTimeMark && mockedTime == other.mockedTime

        override fun hashCode() = mockedTime.hashCode()
    }
}
