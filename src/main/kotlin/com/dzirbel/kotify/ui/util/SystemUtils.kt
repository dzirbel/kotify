package com.dzirbel.kotify.ui.util

import okhttp3.HttpUrl
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URISyntaxException

/**
 * Sets the system clipboard to the given [contents], i.e. "copy". Returns true if successful or false otherwise.
 */
fun setClipboard(contents: String): Boolean {
    @Suppress("SwallowedException")
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(contents), null)
        true
    } catch (ex: IllegalStateException) {
        false
    }
}

/**
 * Returns the current contents of the system clipboard, i.e. "paste".
 */
fun getClipboard(): String {
    return checkNotNull(Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor)) as String
}

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
 * Attempts to open the given [url] in the user's browser, returning true if successful or false otherwise.
 */
fun openInBrowser(url: HttpUrl): Boolean = openInBrowser(url.toUri())

/**
 * Attempts to open the given [uri] in the user's browser, returning true if successful or false otherwise.
 */
fun openInBrowser(uri: URI): Boolean {
    return runCatching { Desktop.getDesktop().browse(uri) }.isSuccess
}
