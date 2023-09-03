package com.dzirbel.kotify

import com.dzirbel.kotify.log.MutableLog
import kotlinx.coroutines.GlobalScope

/**
 * A [MutableLog] for application-wide events.
 */
val EventLog = MutableLog<Unit>(name = "Events", scope = GlobalScope)
