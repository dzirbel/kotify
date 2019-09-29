package com.dominiczirbel

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

/**
 * A simple utility to load secrets from a properties file.
 */
object Secrets {
    private val secrets = Properties()

    fun load() {
        try {
            FileInputStream("config/secrets.properties").use { secrets.load(it) }
        } catch (ex: FileNotFoundException) {
            println("Secrets properties file not found: ${ex.message}")
        }
    }

    operator fun get(name: String): String? = secrets.getProperty(name)
}
