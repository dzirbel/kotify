package com.dzirbel.kotify.ui.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

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
