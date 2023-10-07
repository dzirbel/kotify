package com.dzirbel.kotify

import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.util.coroutines.Computation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.plus

/**
 * A [MutableLog] for application-wide events.
 */
val EventLog = MutableLog<Unit>(name = "Events", scope = GlobalScope.plus(Dispatchers.Computation))
