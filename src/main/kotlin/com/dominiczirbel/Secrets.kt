package com.dominiczirbel

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

/**
 * A simple utility to load secrets from a properties file.
 */
object Secrets {
    private val secrets = Properties()
    private val useEnvVars = runCatching { System.getenv("CI") }.getOrNull() == "true"

    fun load() {
        if (!useEnvVars) {
            try {
                FileInputStream("config/secrets.properties").use { secrets.load(it) }
            } catch (ex: FileNotFoundException) {
                println("Secrets properties file not found: ${ex.message}")
            }
        }
    }

    operator fun get(name: String): String? {
        return if (useEnvVars) System.getenv(name) else secrets.getProperty(name)
    }
}
