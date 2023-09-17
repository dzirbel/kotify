package com.dzirbel.kotify

import com.dzirbel.kotify.Runtime.debug
import java.util.Locale

/**
 * Runtime configuration for the application, in a separate module on which others can depend so top-level application
 * components can use it to control dependent ones (in particular, via [debug]).
 */
object Runtime {
    /**
     * Whether the application is running in debug mode.
     */
    var debug: Boolean = false

    val currentOs: OperatingSystem? by lazy {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        when {
            os.contains("win") -> OperatingSystem.WINDOWS
            os.contains("mac") || os.contains("darwin") -> OperatingSystem.MAC
            os.contains("linux") || os.contains("nix") -> OperatingSystem.LINUX
            else -> null
        }
    }
}

enum class OperatingSystem {
    WINDOWS, MAC, LINUX
}
