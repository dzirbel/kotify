package com.dzirbel.kotify

import com.dzirbel.kotify.Runtime.debug

/**
 * Runtime configuration for the application, in a separate module on which others can depend so top-level application
 * components can use it to control dependent ones (in particular, via [debug]).
 */
object Runtime {
    /**
     * Whether the application is running in debug mode.
     */
    var debug: Boolean = false

    val currentOs: OperatingSystem? by lazy { OperatingSystem.of(System.getProperty("os.name")) }
}
