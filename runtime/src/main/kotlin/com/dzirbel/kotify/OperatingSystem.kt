package com.dzirbel.kotify

import java.util.Locale

enum class OperatingSystem {
    WINDOWS, MAC, LINUX;

    companion object {
        fun of(name: String): OperatingSystem? {
            val os = name.lowercase(Locale.getDefault())
            return when {
                os.contains("win") -> WINDOWS
                os.contains("mac") || os.contains("darwin") -> MAC
                os.contains("linux") || os.contains("nix") -> LINUX
                else -> null
            }
        }
    }
}
