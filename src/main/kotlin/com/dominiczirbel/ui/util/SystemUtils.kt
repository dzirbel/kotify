package com.dominiczirbel.ui.util

import okhttp3.HttpUrl
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URISyntaxException

/**
 * Sets the system clipboard to the given [contents], i.e. "copy".
 */
fun setClipboard(contents: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(contents), null)
}

/**
 * Returns the current contents of the system clipboard, i.e. "paste".
 */
fun getClipboard(): String {
    return Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
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
    // TODO improve error handling, try other ways, etc
    return runCatching { Desktop.getDesktop().browse(uri) }.isSuccess
}
