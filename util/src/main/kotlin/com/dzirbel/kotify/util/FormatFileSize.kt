package com.dzirbel.kotify.util

/**
 * Returns a user-readable string representing the given files size in bytes.
 */
@Suppress("MagicNumber")
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes.toDouble() / (1 shl 10))
        else -> "$bytes bytes"
    }
}
