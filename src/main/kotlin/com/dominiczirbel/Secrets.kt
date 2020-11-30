package com.dominiczirbel

import com.dominiczirbel.network.oauth.OAuth
import kotlinx.coroutines.runBlocking
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

    fun authenticate() {
        val id = get("client_id")!!
        runBlocking {
            OAuth.withClientCredentials(clientId = id, clientSecret = get("client_secret")!!)
        }
        println("Authenticated with client ID $id")
    }

    operator fun get(name: String): String? {
        return if (useEnvVars) System.getenv(name) else secrets.getProperty(name)
    }
}
