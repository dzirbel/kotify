package com.dominiczirbel.util

import java.lang.Long.signum
import java.text.StringCharacterIterator

/**
 * Returns a human-readable format of the given file size in bytes.
 *
 * From https://stackoverflow.com/a/3758880
 */
@Suppress("ImplicitDefaultLocale", "MagicNumber", "UnderscoresInNumericLiterals")
fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }
    var value = bytes
    val ci = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && bytes > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= signum(bytes).toLong()
    return String.format("%.1f %ciB", value / 1024.0, ci.current())
}
