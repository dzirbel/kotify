package com.dzirbel.kotify.repository.util

import com.dzirbel.kotify.util.CurrentTime
import java.time.Instant
import kotlin.time.TimeMark

/**
 * Returns the midpoint between this [TimeMark] and the current time as an [Instant].
 */
fun TimeMark.midpointInstantToNow(): Instant = Instant.ofEpochMilli(midpointTimestampToNow())

/**
 * Returns the midpoint between this [TimeMark] and the current time as milliseconds from the epoch.
 */
fun TimeMark.midpointTimestampToNow(): Long = CurrentTime.millis + elapsedNow().inWholeMilliseconds / 2
