package com.dzirbel.kotify

import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.warn
import com.dzirbel.kotify.util.capitalize
import java.io.File
import java.util.Locale
import java.util.Properties

/**
 * Global constants and configuration for the application.
 */
object Application {
    private const val PROPERTIES_FILENAME = "app.properties"

    // use nullable backing properties so that we can use test values if setup() is not called
    private var _cacheDir: File? = null
    private var _settingsDir: File? = null
    private var _logDir: File? = null

    /**
     * The directory under which cache files should be stored.
     */
    val cacheDir: File by lazy {
        _cacheDir ?: File("../.kotify/test-cache")
            .also { it.mkdirs() }
            .also { EventLog.info("Using test cache directory ${it.absolutePath}") }
    }

    /**
     * The directory under which settings files should be stored.
     */
    val settingsDir: File by lazy {
        _settingsDir ?: File("../.kotify/test-settings")
            .also { it.mkdirs() }
            .also { EventLog.info("Using test settings directory ${it.absolutePath}") }
    }

    val logDir: File by lazy {
        _logDir ?: File("../.kotify/test-logs")
            .also { it.mkdirs() }
            .also { EventLog.info("Using test log directory ${it.absolutePath}") }
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

    private val File.normalizedAbsolutePath: String
        get() = absoluteFile.normalize().path

    fun setupProperties(debug: Boolean) {
        Runtime.debug = debug
        EventLog.info("Initializing in ${if (debug) "DEBUG" else "RELEASE"} mode")

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
    }

    /**
     * Initializes the application-level properties and prints their status to the console.
     */
    fun setup(args: CLIArguments) {
        setupProperties(args.debug)

        val os = Runtime.currentOs
        EventLog.info(
            "Detected operating system: ${os?.name?.lowercase(Locale.getDefault())?.capitalize()} " +
                "(os.name: ${System.getProperty("os.name")})",
        )

        _cacheDir = cacheDirFor(os = os, override = args.cacheDir)
        _settingsDir = settingsDirFor(os = os, override = args.settingsDir)
        _logDir = logDirFor(os = os, override = args.logDir)
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
                ?.also { EventLog.info("Using given cache directory ${it.normalizedAbsolutePath}") }
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
            ?.let { dir ->
                when (os) {
                    OperatingSystem.WINDOWS -> dir.resolve(name).resolve("cache")
                    OperatingSystem.MAC -> dir.resolve(name)
                    OperatingSystem.LINUX -> dir.resolve(".$nameLower").resolve("cache")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved cache")
            ?.also { EventLog.info("Using system cache directory ${it.normalizedAbsolutePath}") }
            ?: File(".").resolve("cache")
                .also { it.mkdirs() }
                .also { EventLog.info("Using backup cache directory ${it.normalizedAbsolutePath}") }
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
                ?.also { EventLog.info("Using given settings directory ${it.normalizedAbsolutePath}") }
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
            ?.let { dir ->
                // resolve a kotify-specific subdirectory
                when (os) {
                    OperatingSystem.WINDOWS -> dir.resolve(name).resolve("settings")
                    OperatingSystem.MAC -> dir.resolve(name)
                    OperatingSystem.LINUX -> dir.resolve(".$nameLower").resolve("settings")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved settings")
            ?.also { EventLog.info("Using system settings directory ${it.normalizedAbsolutePath}") }
            ?: File(".").resolve("settings")
                .also { it.mkdirs() }
                .also { EventLog.info("Using backup settings directory ${it.normalizedAbsolutePath}") }
    }

    /**
     * Resolves the directory to use for log files; this is similar to [cacheDirFor].
     *
     * If [override] is provided, the file it resolves to will be used if it can be created and is writeable. Otherwise,
     * a system directory will be returned if a Kotify-specific subdirectory can be created and is writeable:
     * - Windows: %APPDATA%/Kotify/logs
     * - MacOS:   ~/Library/Application Support/Kotify/logs
     * - Linux:   ~/.kotify/logs
     * If [os] is null or this path cannot be created or is not writeable, a default directory "logs" relative to
     * the working directory will be used.
     */
    private fun logDirFor(os: OperatingSystem?, override: String?): File {
        if (override != null) {
            // first, attempt to return the override directory if given
            File(override)
                .also { it.mkdirs() }
                .takeIfIsWriteableDirectory(directoryName = "given log")
                ?.also { EventLog.info("Using given log directory ${it.normalizedAbsolutePath}") }
                ?.let { return it }
        }

        // the base directory where application settings are stored for os
        val systemSettingsDir = when (os) {
            OperatingSystem.WINDOWS -> appData
            OperatingSystem.MAC -> userHome.resolve("Library").resolve("Application Support")
            OperatingSystem.LINUX -> userHome
            null -> null
        }

        return systemSettingsDir
            ?.takeIfIsWriteableDirectory(directoryName = "system settings")
            ?.let { dir ->
                // resolve a kotify-specific subdirectory
                when (os) {
                    OperatingSystem.WINDOWS -> dir.resolve(name).resolve("logs")
                    OperatingSystem.MAC -> dir.resolve(name).resolve("logs")
                    OperatingSystem.LINUX -> dir.resolve(".$nameLower").resolve("logs")
                    null -> error("impossible")
                }
            }
            ?.also { it.mkdirs() }
            ?.takeIfIsWriteableDirectory(directoryName = "resolved log")
            ?.also { EventLog.info("Using system log directory ${it.normalizedAbsolutePath}") }
            ?: File(".").resolve("logs")
                .also { it.mkdirs() }
                .also { EventLog.info("Using backup log directory ${it.normalizedAbsolutePath}") }
    }

    /**
     * Returns this [File] if it is writeable and a directory, otherwise prints a warning message using [directoryName]
     * and returns null.
     */
    private fun File.takeIfIsWriteableDirectory(directoryName: String): File? {
        return when {
            !isDirectory -> {
                EventLog.warn("${directoryName.capitalize()} directory $normalizedAbsolutePath does not exist")
                null
            }
            !canWrite() -> {
                EventLog.warn("Cannot write to $directoryName directory $normalizedAbsolutePath")
                null
            }
            else -> this
        }
    }
}
