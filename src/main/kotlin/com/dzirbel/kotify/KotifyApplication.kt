package com.dzirbel.kotify

import java.io.File

/**
 * Global constants and configuration for Kotify.
 */
object KotifyApplication {
    enum class OperatingSystem {
        WINDOWS, MAC, LINUX
    }

    val currentOs: OperatingSystem? by lazy {
        val os = System.getProperty("os.name").toLowerCase()
        when {
            os.contains("win") -> OperatingSystem.WINDOWS
            os.contains("mac") || os.contains("darwin") -> OperatingSystem.MAC
            os.contains("linux") || os.contains("nix") -> OperatingSystem.LINUX
            else -> null
        }
    }

    // use nullable backing properties so that we can use test values if setup() is not called
    private var _cacheDir: File? = null
    private var _settingsDir: File? = null

    /**
     * The directory under which cache files should be stored.
     */
    val cacheDir: File by lazy {
        _cacheDir ?: File(".kotify/test-cache").also { println("Using test cache directory ${it.absolutePath}") }
    }

    /**
     * The directory under which settings files should be stored.
     */
    val settingsDir: File by lazy {
        _settingsDir ?: File(".kotify/test-settings").also { println("Using test cache directory ${it.absolutePath}") }
    }

    private val userHome by lazy { File(System.getProperty("user.home")) }
    private val appData by lazy { File(System.getenv("APPDATA")) }

    /**
     * Initializes the application-level properties and prints their status to the console.
     */
    fun setup(cachePath: String? = null, settingsPath: String? = null) {
        val os = currentOs
        println(
            "Detected operating system: ${os?.name?.toLowerCase()?.capitalize()} " +
                "(os.name: ${System.getProperty("os.name")})"
        )

        _cacheDir = cacheDirFor(os = os, override = cachePath)
        _settingsDir = settingsDirFor(os = os, override = settingsPath)
    }

    /**
     * Resolves the directory to use for cache files.
     *
     * If [override] is provided, the file it resolves to will be used if it can be created and is writeable. Otherwise,
     * a system directory will be returned if a Kotify-specific subdirectory can be created and is writeable:
     * - Windows: %APPDATA%/Kotify/cache
     * - MacOS:   ~/Library/Application Support/Kotify
     * - Linux:   ~/.kotify/cache
     * If [os] is null or this path cannot be created or is not writeable, a default directory "cache" relative to
     * the working directory will be used.
     */
    private fun cacheDirFor(os: OperatingSystem?, override: String?): File {
        if (override != null) {
            File(override)
                .also { it.mkdirs() }
                .takeIfIsWriteableDirectory(directoryName = "given cache")
                ?.also { println("Using given cache directory ${it.absolutePath}") }
                ?.let { return it }
        }

        val systemCacheDir = when (os) {
            OperatingSystem.WINDOWS -> appData
            OperatingSystem.MAC -> userHome.resolve("Library").resolve("Application Support")
            OperatingSystem.LINUX -> userHome
            null -> null
        }

        return systemCacheDir
            ?.takeIfIsWriteableDirectory(directoryName = "system cache")
            ?.let {
                when (os) {
                    OperatingSystem.WINDOWS -> it.resolve("Kotify").resolve("cache")
                    OperatingSystem.MAC -> it.resolve("Kotify")
                    OperatingSystem.LINUX -> it.resolve(".kotify").resolve("cache")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved cache")
            ?.also { println("Using system cache directory ${it.absolutePath}") }
            ?: File(".").resolve("cache")
                .also { it.mkdirs() }
                .also { println("Using backup cache directory ${it.absolutePath}") }
    }

    /**
     * Resolves the directory to use for settings files.
     *
     * If [override] is provided, the file it resolves to will be used if it can be created and is writeable. Otherwise,
     * a system directory will be returned if a Kotify-specific subdirectory can be created and is writeable:
     * - Windows: %APPDATA%/Kotify/settings
     * - MacOS:   ~/Library/Preferences/Kotify
     * - Linux:   ~/.kotify/settings
     * If [os] is null or this path cannot be created or is not writeable, a default directory "settings" relative to
     * the working directory will be used.
     */
    private fun settingsDirFor(os: OperatingSystem?, override: String?): File {
        if (override != null) {
            // first, attempt to return the override directory if given
            File(override)
                .also { it.mkdirs() }
                .takeIfIsWriteableDirectory(directoryName = "given settings")
                ?.also { println("Using given settings directory ${it.absolutePath}") }
                ?.let { return it }
        }

        // the base directory where application settings are stored for os
        val systemSettingsDir = when (os) {
            OperatingSystem.WINDOWS -> appData
            OperatingSystem.MAC -> userHome.resolve("Library").resolve("Preferences")
            OperatingSystem.LINUX -> userHome
            null -> null
        }

        return systemSettingsDir
            ?.takeIfIsWriteableDirectory(directoryName = "system settings")
            ?.let {
                // resolve a kotify-specific subdirectory
                when (os) {
                    OperatingSystem.WINDOWS -> it.resolve("Kotify").resolve("settings")
                    OperatingSystem.MAC -> it.resolve("Kotify")
                    OperatingSystem.LINUX -> it.resolve(".kotify").resolve("settings")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved settings")
            ?.also { println("Using system settings directory ${it.absolutePath}") }
            ?: File(".").resolve("settings")
                .also { it.mkdirs() }
                .also { println("Using backup settings directory ${it.absolutePath}") }
    }

    /**
     * Returns this [File] if it is writeable and a directory, otherwise prints a warning message using [directoryName]
     * and returns null.
     */
    private fun File.takeIfIsWriteableDirectory(directoryName: String): File? {
        return when {
            !isDirectory -> {
                println("${directoryName.capitalize()} directory $absolutePath does not exist")
                null
            }
            !canWrite() -> {
                println("Cannot write to $directoryName directory $absolutePath")
                null
            }
            else -> this
        }
    }
}
