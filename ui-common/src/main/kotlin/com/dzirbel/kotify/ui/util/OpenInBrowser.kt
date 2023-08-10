package com.dzirbel.kotify.ui.util

import java.awt.Desktop
import java.net.URI
import java.net.URISyntaxException

/**
 * Attempts to open the given [url] in the user's browser, returning true if successful or false otherwise.
 */
fun openInBrowser(url: String): Boolean {
    val uri = try {
        URI(url)
    } catch (_: URISyntaxException) {
        return false
    }

    return openInBrowser(uri)
}

/**
 * Attempts to open the given [uri] in the user's browser, returning true if successful or false otherwise.
 */
fun openInBrowser(uri: URI): Boolean {
    return runCatching { Desktop.getDesktop().browse(uri) }.isSuccess
}
