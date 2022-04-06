package com.dzirbel.kotify

import com.dzirbel.kotify.util.capitalize
import java.io.File
import java.util.Locale
import java.util.Properties

/**
 * Global constants and configuration for the application.
 */
object Application {
    enum class OperatingSystem {
        WINDOWS, MAC, LINUX
    }

    private const val PROPERTIES_FILENAME = "app.properties"

    val currentOs: OperatingSystem? by lazy {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
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
        _cacheDir ?: File(".kotify/test-cache")
            .also { it.mkdirs() }
            .also { Logger.Events.info("Using test cache directory ${it.absolutePath}") }
    }

    /**
     * The directory under which settings files should be stored.
     */
    val settingsDir: File by lazy {
        _settingsDir ?: File(".kotify/test-settings")
            .also { it.mkdirs() }
            .also { Logger.Events.info("Using test settings directory ${it.absolutePath}") }
    }

    private val userHome by lazy { File(System.getProperty("user.home")) }
    private val appData by lazy { File(System.getenv("APPDATA")) }

    lateinit var name: String
        private set

    private lateinit var nameLower: String

    lateinit var version: String
        private set

    lateinit var github: String
        private set

    /**
     * Initializes the application-level properties and prints their status to the console.
     */
    fun setup(cachePath: String? = null, settingsPath: String? = null) {
        val classLoader = Thread.currentThread().contextClassLoader
        val inputStream = requireNotNull(classLoader.getResourceAsStream(PROPERTIES_FILENAME)) {
            "$PROPERTIES_FILENAME not found"
        }
        val properties = inputStream.use { Properties().apply { load(it) } }

        name = requireNotNull(properties["name"] as? String) { "could not find name property in $PROPERTIES_FILENAME" }
        nameLower = name.lowercase(Locale.getDefault())

        version = requireNotNull(properties["version"] as? String) {
            "could not find version property in $PROPERTIES_FILENAME"
        }

        github = requireNotNull(properties["github"] as? String) {
            "could not find github property in $PROPERTIES_FILENAME"
        }

        val os = currentOs
        Logger.Events.info(
            "Detected operating system: ${os?.name?.lowercase(Locale.getDefault())?.capitalize()} " +
                "(os.name: ${System.getProperty("os.name")})",
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
                ?.also { Logger.Events.info("Using given cache directory ${it.absolutePath}") }
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
                    OperatingSystem.WINDOWS -> it.resolve(name).resolve("cache")
                    OperatingSystem.MAC -> it.resolve(name)
                    OperatingSystem.LINUX -> it.resolve(".$nameLower").resolve("cache")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved cache")
            ?.also { Logger.Events.info("Using system cache directory ${it.absolutePath}") }
            ?: File(".").resolve("cache")
                .also { it.mkdirs() }
                .also { Logger.Events.info("Using backup cache directory ${it.absolutePath}") }
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
                ?.also { Logger.Events.info("Using given settings directory ${it.absolutePath}") }
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
                    OperatingSystem.WINDOWS -> it.resolve(name).resolve("settings")
                    OperatingSystem.MAC -> it.resolve(name)
                    OperatingSystem.LINUX -> it.resolve(".$nameLower").resolve("settings")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved settings")
            ?.also { Logger.Events.info("Using system settings directory ${it.absolutePath}") }
            ?: File(".").resolve("settings")
                .also { it.mkdirs() }
                .also { Logger.Events.info("Using backup settings directory ${it.absolutePath}") }
    }

    /**
     * Returns this [File] if it is writeable and a directory, otherwise prints a warning message using [directoryName]
     * and returns null.
     */
    private fun File.takeIfIsWriteableDirectory(directoryName: String): File? {
        return when {
            !isDirectory -> {
                Logger.Events.warn("${directoryName.capitalize()} directory $absolutePath does not exist")
                null
            }
            !canWrite() -> {
                Logger.Events.warn("Cannot write to $directoryName directory $absolutePath")
                null
            }
            else -> this
        }
    }
}
