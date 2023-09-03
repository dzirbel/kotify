package com.dzirbel.kotify.util

import java.time.Instant
import java.time.ZoneId

/**
 * A wrapper providing access to a system wall-clock as either a timestamp via [CurrentTime.millis] or [Instant] via
 * [CurrentTime.instant].
 *
 * This is preferred over direct calls to [System.currentTimeMillis] et al. to centralize access (thus making it more
 * clear where the system time is being used) and to allow the time to be easily (and without reflection) mocked in
 * tests.
 *
 * TODO add support for generating TimeMarks rather than using TimeSource.Monotonic
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

    val zoneId: ZoneId
        get() {
            check(enabled) { "access to system time is disabled" }
            return mockedZoneId ?: ZoneId.systemDefault()
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
     * Stops mocking calls to [CurrentTime].
     */
    fun closeMock() {
        check(enabled) { "system time was not being mocked" }
        enabled = false
        mockedTime = null
        mockedZoneId = null
    }
}
